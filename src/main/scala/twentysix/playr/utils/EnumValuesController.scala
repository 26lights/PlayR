package twentysix.playr.utils

import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import com.google.common.base.CaseFormat
import play.api.mvc.Action
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import twentysix.playr.simple.ResourceRead
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import twentysix.playr.simple.Resource
import twentysix.playr.simple.ResourceList
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait FilteredEnum {
  this: Enumeration =>
  def inUse: this.ValueSet
}

case class EnumValues(name: String, values: Future[Iterable[JsValue]])
object EnumValues {
  implicit def enumerationToEnumValues(enum: Enumeration) = EnumValues(enum, enum.values)
  implicit def filteredEnumerationToEnumValues(enum: Enumeration with FilteredEnum) = EnumValues(enum, enum.inUse)
  implicit def enumerationTupleToEnumValues[E <: Enumeration](enum: Tuple2[E, E#ValueSet]) =
    EnumValues(enum._1, enum._2)

  def apply[E <: Enumeration](enum: E, values: E#ValueSet): EnumValues =
    EnumValues(enum.toString(), Future.successful(values.map { v: E#Value =>
      JsString(v.toString())
    }))
}

abstract class EnumValuesController(enums: EnumValues*)
    extends Resource[EnumValues]
    with ResourceList
    with ResourceRead {
  val name = "constants"

  val enumMap: Map[String, EnumValues] = enums.map { enum =>
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, enum.name) -> enum
  }.toMap

  def fromId(sid: String) = enumMap.get(sid)

  def list = Action.async { implicit request =>
    implicit val ec = this.controllerComponents.executionContext
    if (request.getQueryString("detailed").map(_ == "true").getOrElse(false)) {
      Future
        .sequence(enumMap.map {
          case (k, v) => v.values.map(values => Json.obj("name" -> v.name, "values" -> values))
        })
        .map { seq =>
          Ok(Json.toJson(seq))
        }
    } else {
      Future.successful(Ok(Json.toJson(enumMap.keys)))
    }

  }

  def read(value: EnumValues) = Action.async {
    implicit val ec = this.controllerComponents.executionContext
    value.values.map { values =>
      Ok(Json.obj("name" -> value.name, "values" -> values))
    }
  }
}
