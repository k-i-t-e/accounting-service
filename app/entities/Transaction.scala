package entities

case class Transaction(from: Option[Account], to: Option[Account], amount: Double)
