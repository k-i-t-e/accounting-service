package repositories

import entities.Transaction

import scala.concurrent.Future

trait TransactionRepository {
  def create(transaction: Transaction): Future[Transaction]

  def getByAccountId(accountId: Long): Future[Seq[Transaction]]
}
