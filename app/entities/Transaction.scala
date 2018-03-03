package entities

import java.util.Date

case class Transaction(from: Option[Account], to: Option[Account], amount: Double, date: Date)
