package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json

import models._

object CompanyController extends RestCrudController[Company] with LoggingFilter{
  val name = "company"

  def fromId(sid: String) = toInt(sid).flatMap(id => CompanyContainer.get(id))

  def list = Action { Ok(Json.toJson(CompanyContainer.list)) }

  def read(company: Company) = Action { Ok(company.name) }

  def delete(company: Company) = Action {
    CompanyContainer.delete(company)
    NoContent
  }

  def write(company: Company) = Action { request =>
    request.body.asText match {
      case Some(name) => Ok(CompanyContainer.update(company.copy(name=name)).name)
      case None       => BadRequest("Invalid name")
    }
  }

  def create = Action { request =>
    request.body.asText match {
      case Some(name) => Created(CompanyContainer.add(name).name)
      case None       => BadRequest("Invalid name")
    }
  }

  def functions(company: Company) = Action {
    val functions = for {
      item <- EmployeeContainer.items.values if item.companyId==company.id
    } yield item.function

    Ok(Json.toJson(functions))
  }
}
