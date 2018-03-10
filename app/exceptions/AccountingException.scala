package exceptions

/**
  * A general business exception for Accounting Service
  * @param message a String of error message
  */
case class AccountingException(message: String) extends RuntimeException
