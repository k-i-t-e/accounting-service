package services.accounting

import java.util.Date

import entities.{Account, Transaction}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import repositories.AccountingRepository

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class DefaultAccountServiceSpec extends Specification with Mockito {
  val testAccount1 = Account(Some(1), "testOwner", 100.0, Some(new Date()))
  val testAccount2 = Account(Some(2), "testOwner", 100.0, Some(new Date()))

  val repositoryMock = mock[AccountingRepository]
  repositoryMock.getAccountById(testAccount1.id.get) returns Future.successful(Some(testAccount1))
  repositoryMock.getAccountById(testAccount2.id.get) returns Future.successful(Some(testAccount2))
  repositoryMock.createTransaction(any[Transaction]) answers(t => {
    val transaction = t.asInstanceOf[Transaction]
    Future.successful(
      transaction.copy(id = Some(new Random().nextLong()))
    )
  })

  val accountingService = new DefaultAccountService(repositoryMock)

  "DefaultAccountService" should {
    "create transaction" in {
      val transaction = Await.result(accountingService.createTransaction(testAccount1.id, testAccount2.id, 50.0), Duration.Inf)
      transaction.id.isDefined must beTrue
      there was two(repositoryMock).getAccountById(anyLong)
      there was one(repositoryMock).createTransaction(any[Transaction])
    }
  }
}
