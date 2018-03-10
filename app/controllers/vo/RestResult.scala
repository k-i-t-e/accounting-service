package controllers.vo

/**
  * A class of Value Objects, that represents results of requests to Application's REST endpoints.
  * Wraps a result with a status and optional message. Can represent either a successful result, or an error. This class
  * is for Controller layer only.
  *
  * @param payload contains a payload of result object. Optional, may not be present if a request was not successful
  * @param status determines if a request was successful
  * @param message an optional parameter, that contains an error message if a request failed with an error
  */
case class RestResult[T](payload: Option[T], status: ResultStatus.Value, message: String = null)

object RestResult {
  /**
    * Creates a successful result
    * @param payload a payload object
    * @return new RestResult object to return from controllers
    */
  def apply[T](payload: T): RestResult[T] = RestResult[T](Some(payload), ResultStatus.OK)

  /**
    * Creates an error RestResult with a payload
    * @param payload a result's payload
    * @param message an error message
    * @return new RestResult object to return from controllers
    */
  def error[T](payload: T, message: String) = RestResult[T](Some(payload), ResultStatus.ERROR, message)

  /**
    * Creates an error RestResult without a payload
    * @param message an error message
    * @return new RestResult object to return from controllers
    */
  def error[T](message: String) = RestResult[T](None, ResultStatus.ERROR, message)
}

object ResultStatus extends Enumeration {
  val OK, ERROR = Value
}
