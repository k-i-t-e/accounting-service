package repositories

import entities.Account

import scala.concurrent.Future

trait AccountingRepository {
  def save(account: Account): Future[Account]

  def delete(accountId: Long): Future[_]

  def getById(accountId: Long): Future[Option[Account]]

  def getByOwner(owner: String): Future[Seq[Account]]
}
