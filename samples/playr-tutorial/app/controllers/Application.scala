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
import play.api.cache.SyncCacheApi
import models.ColorContainer
import models.EmployeeContainer
import models.CompanyContainer
import models.PersonContainer
import models.DaysEnum
import models.MonthEnum
import models.NameEnum

class CrmApi @Inject() (
    val cache: SyncCacheApi,
    controllerComponents: ControllerComponents,
    companyController: CompanyController,
    personController: PersonController
)(implicit employeeContainer: EmployeeContainer, personContainer: models.PersonContainer)
    extends PlayRSubRouter {
  val router = RestApiRouter("crm")
    .add(personController)
    .addResource(companyController) {
      _.add("functions", GET, companyController.functions _)
        .addSubRouter(
          "employee",
          (company: Company) => EmployeeController(controllerComponents, company)
        ) {
          _.add("function", GET, (e: EmployeeController) => e.function _)
        }
    }
}

class ApplicationEnums @Inject()
    extends EnumValuesController(
      DaysEnum,
      MonthEnum -> MonthEnum.values.filter(_.toString().startsWith("J")),
      NameEnum
    )
    with InjectedController

class Application @Inject() (
    crmApi: CrmApi,
    colorController: ColorController,
    enumValuesController: ApplicationEnums
)(
    implicit ec: ExecutionContext
) extends PlayRRouter
    with PlayRInfo {

  val api = RootApiRouter()
    .add(colorController)
    .add(crmApi)
    .add(enumValuesController)
    .withFilter(LoggingFilter)

  val info = Map(
    "info" -> ApiInfo.withController(this),
    "jquery.js" -> JQueryApi.withController(this)
  )
}
