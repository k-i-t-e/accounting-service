package repositories
import java.sql.{SQLException, Timestamp}
import java.util.Date
import javax.inject.{Inject, Singleton}

import entities.{Account, Transaction}
import exceptions.AccountingException
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
    def initialBalance = column[Double]("initial_balance")

    override def * = (id.?, owner, balance, initialBalance, createdDate.?) <> ((applyFromDb _).tupled, unapplyAccountToDb)
  }

  private val accounts = TableQuery[AccountTable]

  /**
    * A mapping for 'operations' table, that holds data on monetary transactions.
    * Unfortunately, here we cannot perform an automatic mapping, because Transaction entity contains nested Account
    * entities, which is impossible to get without joins or additional queries.
    * We also introduce a type alias for an operation table tuple
    */
  type OperationRecord = (Option[Long], Option[Long], Option[Long], Double, Timestamp)
  private class OperationTable(tag: Tag) extends Table[OperationRecord](tag, "operation") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def fromId = column[Long]("from_id")
    def toId = column[Long]("to_id")
    def amount = column[Double]("amount")
    def createdDate = column[Timestamp]("created_date")

    def fromFk = foreignKey("from_id", fromId, accounts)(_.id)
    def toFk = foreignKey("to_id", toId, accounts)(_.id)

    override def * = (id.?, fromId.?, toId.?, amount, createdDate)
  }

  private val operations = TableQuery[OperationTable]

  override def createAccount(account: Account): Future[Account] = {
    val action = (for {
      newId <- accounts.returning(accounts.map(_.id)) += account
    } yield Account(Some(newId), account.owner, account.balance, account.initialBalance, account.createdDate)).transactionally

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
    def accountUpdate(accountId: Long, amount: Double) = {
      sqlu"UPDATE account SET balance = balance + ${amount} WHERE id = ${accountId}" // the feature of "in-place" update
                                                                                      // is not yet supported in Slick,
                                                                                      // so we'll use a plain query
    }

    val params = (
      None,
      transaction.from.flatMap(_.id),
      transaction.to.flatMap(_.id),
      transaction.amount,
      new Timestamp(transaction.date.getTime)
    )

    // Define DB actions to update account balances, to be executed after create transaction query
    val updateActions = Seq(transaction.from.map(a => accountUpdate(a.id.get, -transaction.amount)),
                            transaction.to.map(a => accountUpdate(a.id.get, transaction.amount)))
      .filter(_.isDefined)
      .map(_.get)

    // Persist a transaction and update participating accounts transactionally, in a single DB action
    val action = {
      for {
        newId <- operations.returning(operations.map(_.id)) += params
        _ <- DBIO.seq(updateActions:_*)
      } yield transaction.copy(id = Some(newId))
    }.transactionally.asTry

    // since we've moved the business logic of checking that account balance is positive to the database,
    // we need to catch the possible SQLException from the database and translate it to our business exception
    db.run(action).map {
      case Success(t) => t
      case Failure(e) => e match {
        case sqlException: SQLException => mapException(sqlException)
        case default => throw default;
      }
    }
  }

  private def mapException(exception: SQLException) = {
    if (exception.getMessage.contains("balance_positive")) {
      throw AccountingException("Account balance is not enough")
    } else {
      throw exception
    }
  }

  override def getTransaction(transactionId: Long): Future[Option[Transaction]] = {
    val transactionQuery = operations.filter(_.id === transactionId)
    val fromQuery = transactionQuery.flatMap(_.fromFk)
    val toQuery = transactionQuery.flatMap(_.toFk)
    val actions = transactionQuery.result.headOption zip (
      fromQuery.result.headOption zip toQuery.result.headOption) // join on foreign keys

    db.run(actions).map(r => {
      r._1.map(p => {
        val (id, _, _, amount, date) = p
        val from = r._2._1
        val to = r._2._2
        Transaction(id, from, to, amount, date)
      })
    })
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

      from,
      to
    )

    db.run(joinAction.result).map(
      _.map(r => {
        val (id, amount, date, from, to) = r
        Transaction(Some(id), from, to, amount, date)
      })
    )
  }

  /**
    * A helper method to convert an Account entity to a database table tuple
    */
  private def unapplyAccountToDb(arg: Account): Option[(Option[Long], String, Double, Double, Option[Timestamp])] =
    Some((arg.id, arg.owner, arg.balance, arg.initialBalance, arg.createdDate match {
      case Some(date) => Some(new Timestamp(date.getTime))
      case None => None
    }))

  /**
    * A helper method to construct an Account from values from database
    */
  def applyFromDb(id: Option[Long], owner: String, balance: Double, initialBalance: Double,
                  createdDate: Option[Timestamp]): Account =
    Account(id, owner, balance, initialBalance, createdDate match {
      case Some(timestamp) => Some(new Date(timestamp.getTime))
      case None => None
    })
}
