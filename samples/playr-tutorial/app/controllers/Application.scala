package controllers

import play.api._
import play.api.mvc._
import twentysix.playr.RestApiRouter
import twentysix.playr.RestResourceRouter
import twentysix.playr.GET
import twentysix.playr.ApiInfo
import twentysix.playr.RootApiRouter

object Application extends Controller {

  val crmApi = RestApiRouter("crm")
    .add(PersonController)
    .add(new RestResourceRouter(CompanyController)
      .add("functions", GET, CompanyController.functions _)
      .add("employee", company => EmployeeController(company))
    )

  val api = RootApiRouter()
    .add(ColorController)
    .add(crmApi)

  val apiInfo = ApiInfo(api)
}
