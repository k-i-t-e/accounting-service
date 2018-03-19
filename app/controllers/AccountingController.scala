package controllers

import java.util.Date
import javax.inject.Inject

import controllers.vo.{RestResult, TransactionVO}
import entities.{Account, Transaction}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.accounting.AccountingService

import scala.concurrent.{ExecutionContext, Future}

/**
  * A REST Controller implementation, that handles requests for Account management. Requests are being handled in async
  * way. Renders results as JSON objects.
  *
  * @param cc inject Play's ControllerComponents
  * @param accountingService inject an instance of AccountingService to process requests
  * @param ec inject Play's Default Execution context to tun all Futures on it
  */
class AccountingController @Inject()(cc: ControllerComponents,
                                     accountingService: AccountingService)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  private implicit val accountWrites: Writes[Account] = (
    (JsPath \ "id").writeNullable[Long] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "balance").write[Double] and
    (JsPath \ "createdDate").writeNullable[Date]
  )(account => (account.id, account.owner, account.balance, account.createdDate))

  private implicit val booleanResultWrites: Writes[RestResult[Boolean]] = Json.writes[RestResult[Boolean]]
  private implicit val accountResultWrites: Writes[RestResult[Account]] = Json.writes[RestResult[Account]]
  private implicit val accountSeqResultWrites: Writes[RestResult[Seq[Account]]] = Json.writes[RestResult[Seq[Account]]]
  private implicit val transactionWrites: Writes[Transaction] = Json.writes[Transaction]
  private implicit val transactionResultWrites: Writes[RestResult[Transaction]] = Json.writes[RestResult[Transaction]]
  private implicit val transactionSeqResultWrites: Writes[RestResult[Seq[Transaction]]] =
    Json.writes[RestResult[Seq[Transaction]]]

  private implicit val accountReads: Reads[Account] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "owner").read[String] and
    (JsPath \ "balance").read[Double]
  )((id, owner, balance) => Account(id, owner, balance, balance, None))
  private implicit val transactionVOReads: Reads[TransactionVO] = Json.reads[TransactionVO]

  /**
    * A helper method, that handles JSON parsing errors and return them with a BadRequest status code
    */
  private def throwBadRequestError(error: Seq[(JsPath, scala.Seq[JsonValidationError])]) = Future.successful(
    BadRequest(JsError.toJson(error)))

  def createAccount = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[Account]
      requestContent.fold(
        error => throwBadRequestError(error),
        account => {
          accountingService.createAccount(account).map(a => Ok(Json.toJson(RestResult(a))))
        })
    }
  }

  def updateAccount = Action(parse.json).async {
    request => {
      request.body.validate[Account].fold(
        error => throwBadRequestError(error),
        account => {
          accountingService.updateAccount(account).map(a => Ok(Json.toJson(RestResult(a))))
        }
      )
    }
  }

  def getAccountById(id: Long) = Action.async {
    _ => accountingService.loadAccount(id).map(a => Ok(Json.toJson(RestResult(a))))
  }

  def getAccountByOwner(owner: String) = Action.async {
    _ => accountingService.loadByOwner(owner).map(a => Ok(Json.toJson(RestResult(a))))
  }

  def createTransaction = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[TransactionVO]
      requestContent.fold(
        error => throwBadRequestError(error),
        vo => accountingService.createTransaction(vo.from, vo.to, vo.amount)
          .map(t => Ok(Json.toJson(RestResult(t))))
      )
    }
  }

  def getTransaction(id: Long) = Action.async {
    _ => accountingService.loadTransaction(id).map(t => Ok(Json.toJson(RestResult(t))))
  }

  def getTransactionByAccountId(accountId: Long) = Action.async {
    _ => accountingService.getTransactionsByAccountId(accountId).map(t => Ok(Json.toJson(RestResult(t))))
  }
}
