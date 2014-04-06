package controllers

import play.api.mvc._
import twentysix.playr._
import twentysix.playr.simple._
import play.api.libs.json.Json

case class Person(name: String)

object PersonController extends Controller
                           with Resource[Person]
                           with ResourceRead {
  def name = "person"

  def persons = Map(
    1 -> Person("john"),
    2 -> Person("jane")
  )

  implicit val personFormat = Json.format[Person]

  def fromId(sid: String): Option[Person] = toInt(sid).flatMap(persons.get(_))

  def read(person: Person) = Action { Ok(Json.toJson(person)) }

  def list() = Action { Ok(Json.toJson(persons.keys)) }
}

object PersonRouter extends RestResourceRouter(PersonController)