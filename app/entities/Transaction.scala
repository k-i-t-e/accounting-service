package entities

import java.util.Date

/**
  * An entity object, representing a monetary transaction
  * @param id a Transaction's ID from the database
  * @param from an optional Account, from where a money transfer occurs
  * @param to an optional Account, that is a destination point of a money transfer
  * @param amount an amount of money to be transferred
  * @param date a Date when the transaction occurred
  */
case class Transaction(id: Option[Long],
                       from: Option[Account],
                       to: Option[Account],
                       amount: Double,
                       date: Date)
