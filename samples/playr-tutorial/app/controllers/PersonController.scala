package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json

import models._

object PersonController extends RestCrudController[Person]{
  val name = "person"

  def fromId(sid: String) = toInt(sid).flatMap(id => PersonContainer.get(id))

  def list = Action { Ok(Json.toJson(PersonContainer.list)) }

  def read(person: Person) = Action { Ok(person.name) }

  def delete(person: Person) = Action {
    PersonContainer.delete(person)
    NoContent
  }

  def write(person: Person) = Action { request =>
    request.body.asText match {
      case Some(name) if (name.length > 0) => Ok(PersonContainer.update(person.copy(name=name)).name)
      case _                               => BadRequest("Invalid name")
    }
  }

  def create = Action { request =>
    request.body.asText match {
      case Some(name) if name.length > 0 => Created(PersonContainer.add(name).name)
      case _                             => BadRequest("Invalid name")
    }
  }
}
