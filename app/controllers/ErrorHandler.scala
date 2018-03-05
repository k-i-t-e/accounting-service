package controllers

import javax.inject.Inject

import com.google.inject.Provider
import exceptions.AccountingException
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent.Future

class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {


  override protected def onDevServerError(request: RequestHeader,
                                           exception: UsefulException): Future[Result] = {
    exception.getCause match {
      case AccountingException(message) => Future.successful(Results.BadRequest(message))
      case _ => Future.successful(Results.InternalServerError)
    }
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    onDevServerError(request, exception)
}
