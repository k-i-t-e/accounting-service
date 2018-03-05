package repositories

import entities.{Account, Transaction}

import scala.concurrent.Future

trait AccountingRepository {
  def save(account: Account): Future[Account]

  def delete(accountId: Long): Future[_]

  def getById(accountId: Long): Future[Option[Account]]

  def getByOwner(owner: String): Future[Seq[Account]]

  def createTransaction(transaction: Transaction): Future[Transaction]

  def loadTransaction(transactionId: Long): Future[Option[Transaction]]
}
