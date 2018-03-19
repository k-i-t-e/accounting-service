package repositories

import java.util.Date

import entities.{Account, Transaction}
import exceptions.AccountingException
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{PlaySpecification, WithApplication}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class SlickAccountingRepositoryTest extends PlaySpecification {
  def appWithMemoryDatabase = new GuiceApplicationBuilder().configure(inMemoryDatabase("test")).build()
  "SlickAccountingRepository" should {
    "Account" in {
      "Created and Loaded" in new WithApplication(appWithMemoryDatabase) {
        val repository = app.injector.instanceOf[SlickAccountingRepository]
        repository must beAnInstanceOf[AccountingRepository]

        val account = Account(None, "testOwner", 100.0, 100.0, Some(new Date()))
        val created = Await.result(repository.createAccount(account), Duration.Inf)

        created.id must beSome[Long]
        created.balance must beEqualTo(account.balance)
        created.owner must beEqualTo(account.owner)
        created.createdDate must beEqualTo(account.createdDate)

        val loaded = Await.result(repository.getAccountById(created.id.get), Duration.Inf)
        loaded must beSome[Account]

        val loadedAccount = loaded.get
        loadedAccount.id must beEqualTo(created.id)
        loadedAccount.owner must beEqualTo(account.owner)
        loadedAccount.balance must beEqualTo(account.balance)
        loadedAccount.createdDate must beEqualTo(account.createdDate)
        loadedAccount.initialBalance must beEqualTo(account.initialBalance)

        val loadedAccounts = getFutureResult(repository.getAccountByOwner(account.owner))
        loadedAccounts must not be empty
        loadedAccounts.filter(a => a.id == created.id) must not be empty
        loadedAccounts.filter(a => a.owner == created.owner) must not be empty
        loadedAccounts.filter(a => a.balance == created.balance) must not be empty
        loadedAccounts.filter(a => a.createdDate == created.createdDate) must not be empty
      }
      "Updated" in new WithApplication(appWithMemoryDatabase) {
        val repository = app.injector.instanceOf[SlickAccountingRepository]

        val account = Account(None, "testOwner", 100.0, 100.0, Some(new Date()))
        val created = Await.result(repository.createAccount(account), Duration.Inf)
        val updated = created.copy(balance = 110.0, owner = "newTestOwner", createdDate = Some(new Date()))

        Await.ready(
          repository.updateAccount(updated),
          Duration.Inf
        )

        val loaded = getFutureResult(repository.getAccountById(updated.id.get))
        loaded must beSome[Account]

        val loadedAccount = loaded.get
        loadedAccount.owner must beEqualTo(updated.owner) // this field should be updated

        // These fields should never be updated
        loadedAccount.balance must beEqualTo(created.balance)
        loadedAccount.createdDate must beEqualTo(created.createdDate)
      }
    }
    "Transaction" in {
      "Created and Loaded" in new WithApplication(appWithMemoryDatabase) {
        val repository = app.injector.instanceOf[SlickAccountingRepository]
        val from = getFutureResult(repository.createAccount(Account(None, "testOwner", 100.0, 100.0, Some(new Date()))))
        val to = getFutureResult(repository.createAccount(Account(None, "testOwner", 100.0, 100.0, Some(new Date()))))

        val transaction = Transaction(
          None,
          Some(from.copy(balance = from.balance - 50)), // These operations has to be done in the service layer
          Some(to.copy(balance = to.balance + 50)),
          50.0,
          new Date())
        val created = Await.result(repository.createTransaction(transaction), Duration.Inf)
        created.id must beSome[Long]

        val loaded = getFutureResult(repository.getTransaction(created.id.get))
        loaded must beSome[Transaction]

        val loadedTransaction = loaded.get
        loadedTransaction.id must beEqualTo(created.id)
        loadedTransaction.from must beEqualTo(transaction.from)
        loadedTransaction.to must beEqualTo(transaction.to)
        loadedTransaction.amount must beEqualTo(transaction.amount)
        loadedTransaction.date.getTime must beEqualTo(transaction.date.getTime)

        // Check that accounts were updated
        val loadedFrom = getFutureResult(repository.getAccountById(from.id.get))
        val loadedTo = getFutureResult(repository.getAccountById(to.id.get))

        loadedFrom.get.balance must beEqualTo(from.balance - transaction.amount)
        loadedFrom.get.initialBalance must beEqualTo(from.balance)
        loadedTo.get.balance must beEqualTo(to.balance + transaction.amount)
        loadedTo.get.initialBalance must beEqualTo(to.balance)

        // Test load by account
        val fromTransactionSeq = getFutureResult(repository.getTransactionsByAccountId(from.id.get))
        val toTransactionSeq = getFutureResult(repository.getTransactionsByAccountId(to.id.get))
        fromTransactionSeq must not be empty
        toTransactionSeq must not be empty
        fromTransactionSeq must beEqualTo(toTransactionSeq)
      }
      "Created with one account" in new WithApplication(appWithMemoryDatabase) {
        val repository = app.injector.instanceOf[SlickAccountingRepository]
        val to = getFutureResult(repository.createAccount(Account(None, "testOwner", 100.0, 100.0, Some(new Date()))))

        val transaction = Transaction(
          None,
          None,
          Some(to.copy(balance = to.balance + 50)),
          50.0,
          new Date())
        val created = Await.result(repository.createTransaction(transaction), Duration.Inf)
        created.id must beSome[Long]

        val loaded = getFutureResult(repository.getTransaction(created.id.get))
        loaded must beSome[Transaction]

        val loadedTo = getFutureResult(repository.getAccountById(to.id.get))
        loadedTo.get.balance must beEqualTo(to.balance + transaction.amount)
      }
    }
    "Doesn't allow" in {
      "Transaction" in {
        "Transaction, that exceeds the balance" in new WithApplication(appWithMemoryDatabase) {
          val repository = app.injector.instanceOf[SlickAccountingRepository]
          val from = getFutureResult(repository.createAccount(Account(None, "testOwner", 100.0, 100.0, Some(new Date()))))
          val to = getFutureResult(repository.createAccount(Account(None, "testOwner", 100.0, 100.0, Some(new Date()))))

          val transaction = Transaction(
            None,
            Some(from), // These operations has to be done in the service layer
            Some(to),
            110.0,
            new Date())
          Await.result(
            repository.createTransaction(transaction), Duration.Inf
          ) must throwA[AccountingException]
        }
      }
    }
  }

  private def getFutureResult[T](future: Future[T]): T = {
    Await.result(future, Duration.Inf)
  }
}
