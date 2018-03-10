package controllers

import javax.inject._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's welcome page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Returns a welcome message to signal that service is running
   */
  def index = Action {
    Ok("Accounting Service is ready.")
  }

}
