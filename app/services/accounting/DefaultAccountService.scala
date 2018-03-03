package services.accounting

import javax.inject.{Inject, Singleton}

import entities.{Account, Transaction}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultAccountService @Inject()(implicit ec: ExecutionContext) extends AccountingService {
  override def createAccount(account: Account): Future[Account] = ???

  override def loadAccount(id: Long): Future[Account] = ???

  override def createTransaction(from: Option[Long], to: Option[Long], amount: Double): Future[Transaction] = {

    def loadIfPresent(idOpt: Option[Long]): Future[Option[Account]] = idOpt match {
        case Some(id) => loadAccount(id).map(a => Some(a))
        case None => Future.successful(None)
      }

    for {
      fromOpt <- loadIfPresent(from)
      toOpt <- loadIfPresent(to)
    } yield Transaction(fromOpt, toOpt, amount)
  }
}
