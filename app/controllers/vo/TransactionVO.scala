package controllers.vo

case class TransactionVO(from: Option[Long], to: Option[Long], amount: Double) {
  if (from.isEmpty && to.isEmpty) {
    throw new IllegalArgumentException("Either from or to account should be specified")
  }
}
