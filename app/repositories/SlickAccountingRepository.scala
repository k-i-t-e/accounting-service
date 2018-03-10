package repositories
import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}

import entities.{Account, Transaction}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, Future}

/**
  * An implementation of AccountingRepository, that uses Slick for database interaction.
  * @param dbConfigProvider inject a DatabaseConfigProvider for Slick
  * @param ec inject an ExecutionContext for operations with Futures. For database operations, a Slick's
  *           ExecutionContext is used, but for other actions, we'll use Play's default context.
  */
@Singleton
class SlickAccountingRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                         (implicit ec: ExecutionContext)
  extends AccountingRepository with HasDatabaseConfigProvider[H2Profile] {
  import profile.api._

  /**
    * A mapping for 'account' table, holding data for Accounts. It gets automatically mapped to Account entity
    * @param tag
    */
  private class AccountTable(tag: Tag) extends Table[Account](tag, "account") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[String]("owner")
    def balance = column[Double]("balance")
    def createdDate = column[Timestamp]("created_date")

    override def * = (id.?, owner, balance, createdDate.?) <> ((applyFromDb _).tupled, unapplyAccountToDb)
  }

  private val accounts = TableQuery[AccountTable]

  /**
    * A mapping for 'operations' table, that holds data on monetary transactions.
    * Unfortunately, here we cannot perform an automatic mapping, because Transaction entity contains nested Account
    * entities, which is impossible to get without joins or additional queries.
    * We also introduce a type alias for an operation table tuple
    */
  type OperationRecord = (Option[Long], Option[Long], Option[Long], Double, Timestamp, Option[Double], Option[Double])
  private class OperationTable(tag: Tag) extends Table[OperationRecord](tag, "operation") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def fromId = column[Long]("from_id")
    def toId = column[Long]("to_id")
    def amount = column[Double]("amount")
    def createdDate = column[Timestamp]("created_date")
    def fromBalance = column[Double]("from_balance")  // Contains balance of source account after transaction commit
    def toBalance = column[Double]("to_balance")      // Contains balance of destination account after transaction
                                                      // commit. These fields are used to restore history values of
                                                      // account's balances

    def fromFk = foreignKey("from_id", fromId, accounts)(_.id)
    def toFk = foreignKey("to_id", toId, accounts)(_.id)

    override def * = (id.?, fromId.?, toId.?, amount, createdDate, fromBalance.?, toBalance.?)
  }

  private val operations = TableQuery[OperationTable]

  override def createAccount(account: Account): Future[Account] = {
    val action = (for {
      newId <- accounts.returning(accounts.map(_.id)) += account
    } yield Account(Some(newId), account.owner, account.balance, account.createdDate)).transactionally

    db.run(action)
  }

  override def updateAccount(account: Account): Future[Account] = {
    db.run(
      accounts
        .filter(_.id === account.id)
        .map(_.owner) // allow update of only "owner" field
        .update(account.owner)
    ).map(_ => account)
  }

  override def getAccountById(accountId: Long): Future[Option[Account]] =
    db.run(accounts.filter(_.id === accountId).result.headOption)


  override def getAccountByOwner(owner: String): Future[Seq[Account]] =
    db.run(accounts.filter(_.owner === owner).result)

  override def createTransaction(transaction: Transaction): Future[Transaction] = {
    def accountUpdate(account: Account) = {
      accounts
        .filter(_.id === account.id)
        .map(_.balance)
        .update(account.balance)
    }

    val params = (
      None,
      transaction.from.flatMap(_.id),
      transaction.to.flatMap(_.id),
      transaction.amount,
      new Timestamp(transaction.date.getTime),
      transaction.from.map(_.balance),
      transaction.to.map(_.balance)
    )

    // Define DB actions to update account balances, to be executed after create transaction query
    val updateActions = Seq(transaction.from.map(accountUpdate), transaction.to.map(accountUpdate))
      .filter(_.isDefined)
      .map(_.get)

    // Persist a transaction and update participating accounts transactionally, in a single DB action
    val action = {
      for {
        newId <- operations.returning(operations.map(_.id)) += params
        _ <- DBIO.seq(updateActions:_*)
      } yield transaction.copy(id = Some(newId))
    }.transactionally

    db.run(action)
  }

  override def getTransaction(transactionId: Long): Future[Option[Transaction]] = {
    val transactionQuery = operations.filter(_.id === transactionId)
    val fromQuery = transactionQuery.flatMap(_.fromFk)
    val toQuery = transactionQuery.flatMap(_.toFk)
    val actions = transactionQuery.result.headOption zip (
      fromQuery.result.headOption zip toQuery.result.headOption) // join on foreign keys

    db.run(actions).map(r => {
      r._1.map(p => {
        val (id, _, _, amount, date, fromBalance, toBalance) = p
        val from = r._2._1
        val to = r._2._2
        Transaction(id, from.map(restoreBalanceHistory(_, fromBalance)), to.map(restoreBalanceHistory(_, toBalance)), amount, date)
      })
    })
  }

  private def restoreBalanceHistory(account: Account, historyBalance: Option[Double]) = {
    historyBalance match {
      case Some(b) => account.copy(balance = b)
      case None => account
    }
  }

  override def getTransactionsByAccountId(accountId: Long): Future[Seq[Transaction]] = {
    val joinAction = for {
      ((operation, from), to) <- operations
        .filter(op => op.fromId === accountId || op.toId === accountId)
        .joinLeft(accounts).on(_.fromId === _.id)
        .joinLeft(accounts).on(_._1.toId === _.id)

    } yield (
      operation.id,
      operation.amount,
      operation.createdDate,
      operation.fromBalance.?,
      operation.toBalance.?,

      from,
      to
    )

    db.run(joinAction.result).map(
      _.map(r => {
        val (id, amount, date, fromBalance, toBalance, from, to) = r
        Transaction(Some(id),
                    from.map(restoreBalanceHistory(_, fromBalance)),
                    to.map(restoreBalanceHistory(_, toBalance)),
                    amount,
                    date)
      })
    )
  }

  /**
    * A helper method to convert an Account entity to a database table tuple
    */
  private def unapplyAccountToDb(arg: Account): Option[(Option[Long], String, Double, Option[Timestamp])] =
    Some((arg.id, arg.owner, arg.balance, arg.createdDate match {
      case Some(date) => Some(new Timestamp(date.getTime))
      case None => None
    }))

  /**
    * A helper method to construct an Account from values from database
    */
  def applyFromDb(id: Option[Long],
                  owner: String,
                  balance: Double,
                  createdDate: Option[Timestamp]): Account = new Account(id, owner, balance, createdDate match {
    case Some(timestamp) => Some(new Date(timestamp.getTime))
    case None => None
  })
}
