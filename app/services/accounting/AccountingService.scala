package services.accounting

import entities.{Account, Transaction}

import scala.concurrent.Future

/**
  * An trait of a service class, that incorporates business logic of an Accounting service
  */
trait AccountingService {
  def saveAccount(account: Account): Future[Account]

  def loadAccount(id: Long): Future[Account]

  def loadByOwner(owner: String): Future[Seq[Account]]

  def createTransaction(from: Option[Long], to: Option[Long], amount: Double): Future[Transaction]

  def loadTransaction(id: Long): Future[Transaction]
}
