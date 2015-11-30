package twentysix.playr.core

import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads
import play.api.libs.json.JsError
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.libs.json.JsValue
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsSuccess

trait ResourceShortcuts {
  this: Controller =>
  def ifValidBody[T: Reads](block: T => Result): Request[JsValue] => Result = {
    implicit request => withValidBody[T](block)
  }

  def withValidBody[T: Reads](block: T => Result)(implicit request: Request[JsValue]): Result = {
    request.body.validate[T] match  {
      case s: JsSuccess[T] => block(s.get)
      case e: JsError => BadRequest(JsError.toJson(e))
    }
  }

  def ifValidBodyAsync[T: Reads](block: T => Future[Result]): Request[JsValue] => Future[Result] = {
      implicit request => withValidBodyAsync[T](block)
  }

  def withValidBodyAsync[T: Reads](block: T => Future[Result])(implicit request: Request[JsValue]): Future[Result] = {
    request.body.validate[T] match  {
      case s: JsSuccess[T] => block(s.get)
      case e: JsError => Future(BadRequest(JsError.toJson(e)))
    }
  }

  implicit class ResourceJsonExtensions(value: JsValue) {
    def withValidTransform[A <: JsValue](rds: Reads[A])(block: A => Result): Result = {
      value.transform(rds) match {
        case s: JsSuccess[A] => block(s.get)
        case e: JsError => BadRequest(JsError.toJson(e))
      }
    }

    def withValidTransformAsync[A <: JsValue](rds: Reads[A])(block: A => Future[Result]): Future[Result] = {
        value.transform(rds) match {
        case s: JsSuccess[A] => block(s.get)
        case e: JsError => Future(BadRequest(JsError.toJson(e)))
        }
    }
  }
}
