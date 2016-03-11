package controllers

import play.api.mvc._
import twentysix.playr._
import twentysix.playr.simple._
import play.api.libs.json.Json
import play.api.Logger
import javax.inject.Inject
import com.google.inject.ImplementedBy

case class Person(name: String)

@ImplementedBy(classOf[Persons])
trait PersonList {
  val persons: Map[Int, Person]
}

class Persons extends PersonList{
  val persons = Map(
    1 -> Person("john"),
    2 -> Person("jane")
  )
}


class PersonController @Inject() (personList: PersonList) extends Controller
                                                             with Resource[Person]
                                                             with ResourceRead
                                                             with ResourceList {
  def name = "person"

  implicit val personFormat = Json.format[Person]

  val persons = personList.persons

  def fromId(sid: String): Option[Person] = toInt(sid).flatMap(persons.get(_))

  def read(person: Person) = Action { Ok(Json.toJson(person)) }

  def list() = Action { Ok(Json.toJson(persons.keys)) }
}

class PersonRouter extends RestResourceRouter(new PersonController(new Persons())) with ApiInfo {
  Logger.debug(s"Router instance created.")
}
