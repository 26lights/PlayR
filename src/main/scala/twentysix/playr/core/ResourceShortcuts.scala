package twentysix.playr.core

import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads
import play.api.libs.json.JsError
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import play.api.libs.json.JsValue
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsSuccess

trait ResourceShortcuts {
  this: Controller =>
  def ifValidBody[T: Reads](block: T => SimpleResult): Request[JsValue] => SimpleResult = {
    implicit request => withValidBody[T](block)
  }

  def withValidBody[T: Reads](block: T => SimpleResult)(implicit request: Request[JsValue]): SimpleResult = {
    request.body.validate[T] match  {
      case s: JsSuccess[T] => block(s.get)
      case e: JsError => BadRequest(JsError.toFlatJson(e))
    }
  }

  def ifValidBodyAsync[T: Reads](block: T => Future[SimpleResult]): Request[JsValue] => Future[SimpleResult] = {
      implicit request => withValidBodyAsync[T](block)
  }

  def withValidBodyAsync[T: Reads](block: T => Future[SimpleResult])(implicit request: Request[JsValue]): Future[SimpleResult] = {
    request.body.validate[T] match  {
      case s: JsSuccess[T] => block(s.get)
      case e: JsError => Future(BadRequest(JsError.toFlatJson(e)))
    }
  }

  implicit class ResourceJsonExtensions(value: JsValue) {
    def withValidTransform[A <: JsValue](rds: Reads[A])(block: A => SimpleResult): SimpleResult = {
      value.transform(rds) match {
        case s: JsSuccess[A] => block(s.get)
        case e: JsError => BadRequest(JsError.toFlatJson(e))
      }
    }

    def withValidTransformAsync[A <: JsValue](rds: Reads[A])(block: A => Future[SimpleResult]): Future[SimpleResult] = {
        value.transform(rds) match {
        case s: JsSuccess[A] => block(s.get)
        case e: JsError => Future(BadRequest(JsError.toFlatJson(e)))
        }
    }
  }
}