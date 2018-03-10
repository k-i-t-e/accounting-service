package services.accounting

import entities.{Account, Transaction}

import scala.concurrent.Future

/**
  * An trait of a service class, that incorporates business logic of an Accounting service. Due to a reactive nature
  * of an application, all methods return futures.
  */
trait AccountingService {
  /**
    * Persists an Account entity
    * @param account an Account to persist
    * @return a Future of newly created entity
    */
  def createAccount(account: Account): Future[Account]

  /**
    * Updates an existing Account entity. Only 'owner' field is allowed for updating, other fields are
    * ignored. If no Account with the given ID was found, throws an AccountingException
    * @param account an Account to update
    * @return a Future of updated Account
    */
  def updateAccount(account: Account): Future[Account]

  /**
    * Retrieves an Account by it's ID. If no Account with the given ID was found, throws an AccountingException
    * @param id an ID of account tot retrieve
    * @return an Future of Account
    */
  def loadAccount(id: Long): Future[Account]

  /**
    * Retrieves all Accounts, that belongs to a give owner
    * @param owner an owner, whose accounts to load
    * @return a Future of a sequence of Accounts
    */
  def loadByOwner(owner: String): Future[Seq[Account]]

  /**
    * Creates a monetary transaction, transferring a give amount of money from one given account to another.
    * Updates the balances of Accounts, specified by given IDs. At least one Account ID should be specified, otherwise
    * an AccountingException will be thrown.
    * @param from an ID of account to transfer money from. If not specified, the transaction is counted as account
    *             replenishment
    * @param to an ID of account to transfer money to. If not specified, the transaction is counted as a money
    *           withdrawal
    * @param amount an amount of money to transfer. Has to be greater than zero
    * @return a Future of newly created Transaction
    */
  def createTransaction(from: Option[Long], to: Option[Long], amount: Double): Future[Transaction]

  /**
    * Loads a Transaction by it's ID
    * @param id an ID of Transaction to load
    * @return a Future of loaded Transaction
    */
  def loadTransaction(id: Long): Future[Transaction]

  /**
    * Loads all transactions, where a given Account takes part.
    * @param accountId an ID of account
    * @return a Future of Sequence of Transactions
    */
  def getTransactionsByAccountId(accountId: Long): Future[Seq[Transaction]]
}
