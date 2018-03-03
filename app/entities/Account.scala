package entities

import java.sql.Timestamp
import java.util.Date

/**
  * An entity, that represents a Banking Account
  * @param id of an account in the database
  * @param owner account's owner
  * @param balance account's balance
  * @param createdDate date of creation
  */
case class Account(id: Option[Long],
                   owner: String,
                   balance: Double,
                   createdDate: Option[Date])

object Account {
  def unapplyToDb(arg: Account): Option[(Option[Long], String, Double, Option[Timestamp])] =
    Some((arg.id, arg.owner, arg.balance, arg.createdDate match {
      case Some(date) => Some(new Timestamp(date.getTime))
      case None => None
    }))

  def applyFromDb(id: Option[Long],
            owner: String,
            balance: Double,
            createdDate: Option[Timestamp]): Account = new Account(id, owner, balance, createdDate match {
    case Some(timestamp) => Some(new Date(timestamp.getTime))
    case None => None
  })
}
