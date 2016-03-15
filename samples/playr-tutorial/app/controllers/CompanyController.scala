package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json
import models._
import play.api.cache.CacheApi

class CompanyController(implicit companyContainer: CompanyContainer, employeeContainer: EmployeeContainer) extends RestCrudController[Company] with LoggingFilter{
  val name = "company"

  def fromId(sid: String) = toInt(sid).flatMap(id => companyContainer.get(id))

  def list = Action { Ok(Json.toJson(companyContainer.list)) }

  def read(company: Company) = Action { Ok(company.name) }

  def delete(company: Company) = Action {
    companyContainer.delete(company)
    NoContent
  }

  def write(company: Company) = Action { request =>
    request.body.asText match {
      case Some(name) => Ok(companyContainer.update(company.copy(name=name)).name)
      case None       => BadRequest("Invalid name")
    }
  }

  def create = Action { request =>
    request.body.asText match {
      case Some(name) => Created(companyContainer.add(name).name)
      case None       => BadRequest("Invalid name")
    }
  }

  def functions(company: Company) = Action {
    val functions = for {
      item <- employeeContainer.items.values if item.companyId==company.id
    } yield item.function

    Ok(Json.toJson(functions))
  }
}
