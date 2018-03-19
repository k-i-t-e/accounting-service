package services.accounting

import java.util.Date

import entities.{Account, Transaction}
import exceptions.AccountingException
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import repositories.AccountingRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class DefaultAccountServiceSpec extends Specification with Mockito {
  private val TEST_NON_EXISTING_TRANSACTION_ID = 999
  private val TEST_NON_EXISTING_ACCOUNT_ID = 999

  private val testAccount1 = Account(Some(1), "testOwner", 100.0, 100.0, Some(new Date()))
  private val testAccount2 = Account(Some(2), "testOwner", 100.0, 100.0, Some(new Date()))

  private val repositoryMock = mock[AccountingRepository]
  repositoryMock.getAccountById(testAccount1.id.get) returns Future.successful(Some(testAccount1))
  repositoryMock.getAccountById(testAccount2.id.get) returns Future.successful(Some(testAccount2))
  repositoryMock.getAccountById(TEST_NON_EXISTING_ACCOUNT_ID) returns Future.successful(None)
  repositoryMock.getTransaction(TEST_NON_EXISTING_TRANSACTION_ID) returns Future.successful(None) // some
  repositoryMock.updateAccount(any[Account]) answers (a => Future.successful(a.asInstanceOf[Account]))
  repositoryMock.createTransaction(any[Transaction]) answers(t => {
    val transaction = t.asInstanceOf[Transaction]
    val from = transaction.from.map(a => a.copy(balance = a.balance - transaction.amount))
    val to = transaction.to.map(a => a.copy(balance = a.balance + transaction.amount))
    Future.successful(
      transaction.copy(id = Some(new Random().nextLong()), from = from, to = to)
    )
  })
  repositoryMock.createAccount(any[Account]) answers (
    account => Future.successful(account
      .asInstanceOf[Account]
      .copy(id = Some(new Random().nextLong())))
  )

  private val accountingService = new DefaultAccountService(repositoryMock)

  "DefaultAccountService" should {
    "Create Transaction" in {
      val amount = 50.0
      val transaction = Await.result(accountingService.createTransaction(testAccount1.id, testAccount2.id, amount), Duration.Inf)
      transaction.id must beSome[Long]
      transaction.from.get.balance mustEqual (testAccount1.balance - amount)
      transaction.to.get.balance mustEqual (testAccount2.balance + amount)
    }
    "Create Transaction with one participant" in {
      val amount = 50.0
      val transaction = Await.result(
        accountingService.createTransaction(testAccount1.id, None, amount), Duration.Inf
      ) // A withdrawal transaction

      transaction.id must beSome[Long]
      transaction.from.get.balance mustEqual (testAccount1.balance - amount)
    }
    "Set created date on Account creation" in {
      val created = Await.result(
        accountingService.createAccount(Account(None, "testOwner", 100.0, 100.0, None)), Duration.Inf
      )
      created.id must beSome[Long]
      created.createdDate must beSome[Date]
    }
    "Don't allow" in {
      "Account update without an ID" in {
        Await.result(
          accountingService.updateAccount(Account(None, "testOwner", 100.0, 100.0, None)), Duration.Inf
        ) must throwA[AccountingException]
      }
      "Update of a non existing Account" in {
        Await.result(
          accountingService.updateAccount(Account(Some(TEST_NON_EXISTING_ACCOUNT_ID), "testOwner", 100.0, 100.0, None)),
          Duration.Inf
        ) must throwA[AccountingException]
      }
      "Transaction with no participant accounts" in {
        Await.result(
          accountingService.createTransaction(None, None, 50.0), Duration.Inf
        ) must throwA[AccountingException]
      }
      "Loading of a non-exisitng Transaction" in {
        Await.result(
          accountingService.loadTransaction(TEST_NON_EXISTING_TRANSACTION_ID), Duration.Inf
        ) must throwA[AccountingException]
      }
    }
  }
}
