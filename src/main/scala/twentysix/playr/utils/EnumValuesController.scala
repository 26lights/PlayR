package twentysix.playr.utils

import scala.reflect.runtime.universe._
import com.google.common.base.CaseFormat
import play.api.mvc.Action
import twentysix.playr.simple.ResourceRead
import play.api.libs.json.Json
import twentysix.playr.simple.Resource
import twentysix.playr.simple.ResourceList

case class EnumValuesController(enums: Enumeration*) extends Resource[Enumeration]
                                                        with ResourceList
                                                        with ResourceRead{
  val name = "constants"

  val enumMap: Map[String, Enumeration] = enums.map{ enum =>
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, enum.toString()) -> enum
  }.toMap

  def fromId(sid: String) = enumMap.get(sid)

  def list = Action { Ok(Json.toJson(enumMap.keys)) }

  def read(value: Enumeration) = Action{ Ok(Json.obj("name" -> value.toString(), "values" -> value.values.map(_.toString()))) }
}
