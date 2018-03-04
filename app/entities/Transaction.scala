package entities

import java.util.Date

case class Transaction(id: Option[Long], from: Option[Account], to: Option[Account], amount: Double, date: Date)
