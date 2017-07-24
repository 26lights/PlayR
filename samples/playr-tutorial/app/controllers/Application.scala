package controllers

import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import twentysix.playr.RestApiRouter
import twentysix.playr.RestResourceRouter
import twentysix.playr.GET
import twentysix.playr.ApiInfo
import twentysix.playr.RootApiRouter
import twentysix.playr.SubRestResourceRouter
import twentysix.playr.utils.EnumValuesController
import models.Company
import javax.inject.Inject
import twentysix.playr.di._
import twentysix.playr.RestApiRouter
import play.api.cache.CacheApi
import models.ColorContainer
import models.EmployeeContainer
import models.CompanyContainer
import models.PersonContainer
import models.DaysEnum
import models.MonthEnum
import models.NameEnum

class CrmApi @Inject()(val cache: CacheApi) extends PlayRSubRouter{
  implicit val employeeContainer = EmployeeContainer(cache)
  implicit val personContainer = PersonContainer(cache)
  implicit val companyContainer = CompanyContainer(cache)

  val companyController = new CompanyController

  val router = RestApiRouter("crm")
    .add(new PersonController)
    .addResource(companyController) { _
      .add("functions", GET, companyController.functions _)
      .addSubRouter("employee", (company: Company) => EmployeeController(company)) { _
        .add("function", GET, (e: EmployeeController) => e.function _)
      }
    }
}

class Application @Inject()(val cache: CacheApi, crmApi: CrmApi)(implicit ec: ExecutionContext) extends PlayRRouter with PlayRInfo {

  implicit val colorContainer = ColorContainer(cache)

  val api = RootApiRouter()
    .add(new ColorController)
    .add(crmApi)
    .add(EnumValuesController(
        DaysEnum,
        MonthEnum -> MonthEnum.values.filter(_.toString().startsWith("J")),
        NameEnum
    ))
    .withFilter(LoggingFilter)

  val info = Map(
    "info" -> ApiInfo,
    "jquery.js" -> JQueryApi
  )
}
