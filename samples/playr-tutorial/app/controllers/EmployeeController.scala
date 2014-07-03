package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json

import models._

case class EmployeeController(company: Company) extends RestRwdController[Employee] with LoggingFilter{
  val name = "employee"

  implicit val employeeFormat = Json.format[Employee]

  def fromId(sid: String) = toInt(sid).flatMap(id => EmployeeContainer.get(id))

  def list = Action { Ok(Json.toJson(EmployeeContainer.filterList(_.companyId==company.id))) }

  def read(employee: Employee) = Action { Ok(Json.toJson(employee)) }

  def delete(employee: Employee) = Action {
    EmployeeContainer.delete(employee)
    NoContent
  }

  def update(employee: Employee) = Action { request =>
    request.body.asText match {
      case Some(function) =>
        Ok(Json.toJson(EmployeeContainer.update(employee.copy(function=function))))
      case None           => BadRequest("Invalid name")
    }
  }

  def create = Action(parse.json) { request =>
    val employee = for {
      personId <- (request.body \ "personId").asOpt[Int]
      person <- PersonContainer.get(personId)
      function <- (request.body \ "function").asOpt[String]
    } yield EmployeeContainer.add(company, person, function)

    employee.map(e=>Created(Json.toJson(e))).getOrElse(BadRequest)
  }

  def function(employee: Employee) = Action( Ok(employee.function) )
}
