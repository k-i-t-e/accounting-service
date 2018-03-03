package repositories
import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import entities.Account
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickAccountingRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                         (implicit ec: ExecutionContext)
  extends AccountingRepository with HasDatabaseConfigProvider[H2Profile] {
  import profile.api._

  private class AccountTable(tag: Tag) extends Table[Account](tag, "account") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[String]("owner")
    def balance = column[Double]("balance")
    def createdDate = column[Timestamp]("created_date")

    override def * = (id.?, owner, balance, createdDate.?) <> ((Account.applyFromDb _).tupled, Account.unapplyToDb)
  }

  private val accounts = TableQuery[AccountTable]

  override def save(account: Account): Future[Account] = { // TODO: add update
    val action = (for {
      newId <- accounts.returning(accounts.map(_.id)) += account
    } yield Account(Some(newId), account.owner, account.balance, account.createdDate)).transactionally

    db.run(action)
  }

  override def delete(accountId: Long): Future[_] = db.run(accounts.filter(_.id === accountId).delete.transactionally)


  override def getById(accountId: Long): Future[Option[Account]] =
    db.run(accounts.filter(_.id === accountId).result.headOption)


  override def getByOwner(owner: String): Future[Seq[Account]] =
    db.run(accounts.filter(_.owner === owner).result)
}
