package services.accounting

import java.util.Date
import javax.inject.{Inject, Singleton}

import entities.{Account, Transaction}
import exceptions.AccountingException
import repositories.AccountingRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultAccountService @Inject()(accountingRepository: AccountingRepository)
                                     (implicit ec: ExecutionContext) extends AccountingService {
  override def saveAccount(account: Account): Future[Account] = {
    account.id.map(loadAccount) // when updating, makes sure the account being updated exists
    accountingRepository.save(Account(account.id, account.owner, account.balance, Some(new Date())))
  }

  override def loadAccount(id: Long): Future[Account] = accountingRepository.getById(id).map({
    case Some(a) => a
    case None => throw new AccountingException("No such account exists")
  })

  override def createTransaction(from: Option[Long], to: Option[Long], amount: Double): Future[Transaction] = {

    def loadIfPresent(idOpt: Option[Long]): Future[Option[Account]] = idOpt match {
        case Some(id) => loadAccount(id).map(a => Some(a))
        case None => Future.successful(None)
      }

    val transaction = for {
      fromOpt <- loadIfPresent(from)
      toOpt <- loadIfPresent(to)
    } yield Transaction(None,
      fromOpt.map(from => processTransaction(from, -amount)),
      toOpt.map(to => processTransaction(to, amount)),
      amount,
      new Date())

    transaction.flatMap(accountingRepository.createTransaction)
  }

  private def processTransaction(account: Account, amount: Double): Account = {
    if (amount < 0 && account.balance < -amount) {
      throw new AccountingException("Account balance is not enough")
    }

    account.copy(balance = account.balance + amount)
  }

  override def loadByOwner(owner: String):Future[Seq[Account]] = accountingRepository.getByOwner(owner)

  override def loadTransaction(id: Long): Future[Transaction] = accountingRepository.loadTransaction(id).map({
    case Some(t) => t
    case None => throw new AccountingException("No such transaction exists")
  })
}
