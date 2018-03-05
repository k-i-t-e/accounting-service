package repositories

import entities.{Account, Transaction}

import scala.concurrent.Future

trait AccountingRepository {
  def createAccount(account: Account): Future[Account]

  def updateAccount(account: Account): Future[Account]

  def deleteAccount(accountId: Long): Future[_]

  def getAccountById(accountId: Long): Future[Option[Account]]

  def getAccountByOwner(owner: String): Future[Seq[Account]]

  def createTransaction(transaction: Transaction): Future[Transaction]

  def getTransaction(transactionId: Long): Future[Option[Transaction]]

  def getTransactionsByAccountId(accountId: Long): Future[Seq[Transaction]]
}
