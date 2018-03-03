package controllers.vo

case class RestResult[T](payload: Option[T], status: ResultStatus.Value, message: String = null)

object RestResult {
  def apply[T](payload: T): RestResult[T] = RestResult[T](Some(payload), ResultStatus.OK)
  def error[T](payload: T, message: String) = RestResult[T](Some(payload), ResultStatus.ERROR, message)
  def error[T](message: String) = RestResult[T](None, ResultStatus.ERROR, message)
}

object ResultStatus extends Enumeration {
  val OK, ERROR = Value
}
