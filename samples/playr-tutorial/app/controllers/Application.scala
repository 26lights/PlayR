package controllers

import play.api._
import play.api.mvc._
import twentysix.playr.RestApiRouter
import twentysix.playr.RestResourceRouter
import twentysix.playr.GET
import twentysix.playr.ApiInfo
import twentysix.playr.RootApiRouter
import twentysix.playr.SubRestResourceRouter
import models.Company
import javax.inject.Inject
import twentysix.playr.di._
import twentysix.playr.RestApiRouter

class Application extends PlayRRouter with PlayRInfo {

  val employeeApi = new SubRestResourceRouter[CompanyController.type, EmployeeController]("employee", (company: Company) => EmployeeController(company))
    .add("function", GET, (e: EmployeeController) => e.function _)

  val crmApi = RestApiRouter("crm")
    .add(PersonController)
    .add(new RestResourceRouter(CompanyController)
      .add("functions", GET, CompanyController.functions _)
      .add(employeeApi)
    )

  val api = RootApiRouter()
    .add(ColorController)
    .add(crmApi)

  val info = Map("info" -> ApiInfo, "jquery.js" -> JQueryApi)
}
