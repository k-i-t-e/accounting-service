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
  override def createAccount(account: Account): Future[Account] = {
    accountingRepository.createAccount(account.copy(createdDate = Some(new Date())))
  }

  override def updateAccount(account: Account): Future[Account] = {
    account.id match {
      case Some(id) => for {
          oldAccount <- loadAccount(id) // when updating, makes sure the account being updated exists
          updated <- accountingRepository.updateAccount(oldAccount.copy(owner = account.owner))
        } yield updated
      case None => throw AccountingException("No account ID specified")
    }
  }

  override def loadAccount(id: Long): Future[Account] = accountingRepository.getAccountById(id).map({
    case Some(a) => a
    case None => throw AccountingException("No such account exists")
  })

  override def createTransaction(from: Option[Long], to: Option[Long], amount: Double): Future[Transaction] = {
    def loadIfPresent(idOpt: Option[Long]): Future[Option[Account]] = idOpt match {
        case Some(id) => loadAccount(id).map(a => Some(a))
        case None => Future.successful(None)
      }

    if (from.isEmpty && to.isEmpty) {
      throw AccountingException("A Transaction should have either 'from' or 'to' participant")
    }
    if (amount <= 0) {
      throw AccountingException("A Transaction's amount should be positive")
    }

    val transaction = for {
      fromOpt <- loadIfPresent(from)
      toOpt <- loadIfPresent(to)
    } yield Transaction(None,
      fromOpt,
      toOpt,
      amount,
      new Date())

    transaction.flatMap(accountingRepository.createTransaction)
  }

  override def loadByOwner(owner: String): Future[Seq[Account]] = accountingRepository.getAccountByOwner(owner)

  override def loadTransaction(id: Long): Future[Transaction] = accountingRepository.getTransaction(id).map({
    case Some(t) => t
    case None => throw AccountingException("No such transaction exists")
  })

  override def getTransactionsByAccountId(accountId: Long): Future[Seq[Transaction]] =
    accountingRepository.getTransactionsByAccountId(accountId)
}
