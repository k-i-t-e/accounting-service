package controllers

import java.util.Date
import javax.inject.Inject

import controllers.vo.{RestResult, TransactionVO}
import entities.Account
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.accounting.AccountingService

import scala.concurrent.{ExecutionContext, Future}

class AccountingController @Inject()(cc: ControllerComponents,
                                     accountingService: AccountingService)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val accountWrites = Json.writes[Account]
  implicit val accountResultWrites = Json.writes[RestResult[Account]]

  implicit val accountReads: Reads[Account] = Json.reads[Account]
  implicit val transactionVOReads: Reads[TransactionVO] = Json.reads[TransactionVO]

  /**
    * Validates incoming data. returns BadRequest if something is wrong
    */
  //private def validateAccount[A : Reads] = parse.json.validate[A](_.validate.asEither.left.map(e => BadRequest(JsError.toJson(e))))
  //private def validateTransaction = parse.json.validate[TransactionVO](v => v.validate.asEither.left.map(e => BadRequest(JsError.toJson(e))))

  private def badRequestError(error: Seq[(JsPath, scala.Seq[JsonValidationError])]) = Future.successful(BadRequest(JsError.toJson(error)))

  def createAccount = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[Account]
      requestContent.fold(
        badRequestError,
        account => {
          Future.successful(Ok(Json.toJson(RestResult[Account](Account(None, account.owner, account.balance, Some(new Date()))))))
        })
    }
  }

  def getAccount(id: Long) = Action.async {
    Future.successful(InternalServerError)
  }

  def getAccount(owner: String) = Action.async {
    Future.successful(InternalServerError)
  }

  def createTransaction = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[TransactionVO]
      requestContent.fold(
        badRequestError,
        vo => Future.successful(Ok("ololo"))
      )
    }
  }
}
