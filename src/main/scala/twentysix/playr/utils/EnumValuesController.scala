package twentysix.playr.utils

import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import com.google.common.base.CaseFormat
import play.api.mvc.Action
import twentysix.playr.simple.ResourceRead
import play.api.libs.json.Json
import twentysix.playr.simple.Resource
import twentysix.playr.simple.ResourceList

trait FilteredEnum {
  this: Enumeration =>
  def inUse: this.ValueSet
}

case class EnumValues(name: String, values: Set[String])
object EnumValues {
  implicit def enumerationToEnumValues(enum: Enumeration) = EnumValues(enum, enum.values)
  implicit def filteredEnumerationToEnumValues(enum: Enumeration with FilteredEnum) = EnumValues(enum, enum.inUse)
  implicit def enumerationTupleToEnumValues[E<:Enumeration](enum: Tuple2[E, E#ValueSet]) = EnumValues(enum._1, enum._2)

  def apply[E<:Enumeration](enum: E, values: E#ValueSet): EnumValues =
    EnumValues(enum.toString(), values.map{ v:E#Value => v.toString() })
}

case class EnumValuesController(enums: EnumValues*) extends Resource[EnumValues]
                                                       with ResourceList
                                                       with ResourceRead{
  val name = "constants"

  val enumMap: Map[String, EnumValues] = enums.map{ enum =>
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, enum.name) -> enum
  }.toMap

  def fromId(sid: String) = enumMap.get(sid)

  def list = Action { Ok(Json.toJson(enumMap.keys)) }

  def read(value: EnumValues) = Action{ Ok(Json.obj("name" -> value.name, "values" -> value.values)) }
}
