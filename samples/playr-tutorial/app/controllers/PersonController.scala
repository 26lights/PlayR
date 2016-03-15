package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json
import models._
import play.api.cache.CacheApi

class PersonController(implicit personContainer: PersonContainer) extends RestCrudController[Person]{
  val name = "person"

  def fromId(sid: String) = toInt(sid).flatMap(id => personContainer.get(id))

  def list = Action { Ok(Json.toJson(personContainer.list)) }

  def read(person: Person) = Action { Ok(person.name) }

  def delete(person: Person) = Action {
    personContainer.delete(person)
    NoContent
  }

  def write(person: Person) = Action { request =>
    request.body.asText match {
      case Some(name) if (name.length > 0) => Ok(personContainer.update(person.copy(name=name)).name)
      case _                               => BadRequest("Invalid name")
    }
  }

  def create = Action { request =>
    request.body.asText match {
      case Some(name) if name.length > 0 => Created(personContainer.add(name).name)
      case _                             => BadRequest("Invalid name")
    }
  }
}
