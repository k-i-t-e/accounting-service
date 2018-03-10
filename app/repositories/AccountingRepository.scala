package repositories

import entities.{Account, Transaction}

import scala.concurrent.Future

/**
  * A trait, that represents a repository for managing accounting data: Account and Transaction entities.
  * This repository is reactive: all it's methods return Futures. This repository mixes methods, related to both
  * Accounts and Transactions, because these actions are strictly connected. To achieve atomicity in such operations
  * (e.g. when creating a Transaction while updating the participating accounts), we have to perform it in a
  * single DB action.
  */
trait AccountingRepository {
  /**
    * Persists an Account entity to the database
    * @param account an Account to persist
    * @return a Future of newly created entity
    */
  def createAccount(account: Account): Future[Account]

  /**
    * Updates an existing Account entity in the database. Only 'owner' field is allowed for updating, other fields are
    * ignored
    * @param account an Account to update
    * @return a Future of updated Account
    */
  def updateAccount(account: Account): Future[Account]

  /**
    * Retrieves an Account from the database by it's ID
    * @param accountId an ID of account tot retrieve
    * @return an Future of optional Account from the database. If an Account is not present, the value will be None
    */
  def getAccountById(accountId: Long): Future[Option[Account]]

  /**
    * Retrieves all Accounts, that belongs to a give owner from the database
    * @param owner an owner, whose accounts to load
    * @return a Future of a sequence of Accounts
    */
  def getAccountByOwner(owner: String): Future[Seq[Account]]

  /**
    * Persists a monetary transaction to the database, updating the balances of Accounts, participating in the given
    * Transaction. This has to be done in a single method to achieve atomicity
    * @param transaction a Transaction to persist
    * @return a Future of newly created Transaction
    */
  def createTransaction(transaction: Transaction): Future[Transaction]

  /**
    * Loads a Transaction from the database by it's ID
    * @param transactionId an ID of Transaction to load
    * @return a Future of loaded Transaction
    */
  def getTransaction(transactionId: Long): Future[Option[Transaction]]

  /**
    * Loads all transactions, where a given Account takes part.
    * @param accountId an ID of account
    * @return a Future of Sequence of Transactions
    */
  def getTransactionsByAccountId(accountId: Long): Future[Seq[Transaction]]
}
