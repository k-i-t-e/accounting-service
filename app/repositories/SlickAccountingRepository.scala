package repositories
import entities.Account

class SlickAccountingRepository extends AccountingRepository {
  override def save(account: Account) = ???

  override def delete(accountId: Long) = ???

  override def getById(accountId: Long) = ???

  override def getByOwner(owner: String) = ???
}
