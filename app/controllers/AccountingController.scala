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

class AccountingController @Inject()(cc: ControllerComponents,
                                     accountingService: AccountingService)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val accountWrites = Json.writes[Account]
  implicit val accountResultWrites = Json.writes[RestResult[Account]]
  implicit val accountSeqResultWrites = Json.writes[RestResult[Seq[Account]]]
  implicit val transactionWrites = Json.writes[Transaction]
  implicit val transactionResultWrites = Json.writes[RestResult[Transaction]]

  implicit val accountReads: Reads[Account] = Json.reads[Account]
  implicit val transactionVOReads: Reads[TransactionVO] = Json.reads[TransactionVO]

  private def throwBadRequestError(error: Seq[(JsPath, scala.Seq[JsonValidationError])]) = Future.successful(BadRequest(JsError.toJson(error)))

  def createAccount = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[Account]
      requestContent.fold(
        throwBadRequestError,
        account => {
          accountingService.saveAccount(account).map(a => Ok(Json.toJson(RestResult(a))))
        })
    }
  }

  def getAccountById(id: Long) = Action.async {
    _ => {
      accountingService.loadAccount(id).map(a => Ok(Json.toJson(RestResult(a))))
    }
  }

  def getAccountByOwner(owner: String) = Action.async {
    _ => {
      accountingService.loadByOwner(owner).map(a => Ok(Json.toJson(RestResult(a))))
    }
  }

  def createTransaction = Action(parse.json).async {
    request => {
      val requestContent = request.body.validate[TransactionVO]
      requestContent.fold(
        throwBadRequestError,
        vo => accountingService.createTransaction(vo.from, vo.to, vo.amount)
          .map(t => Ok(Json.toJson(RestResult(t))))
      )
    }
  }
}
