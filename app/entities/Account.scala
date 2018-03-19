package entities

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
                   initialBalance: Double,
                   createdDate: Option[Date])
