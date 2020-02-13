package twentysix.playr

import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.Logger
import twentysix.playr.RestRouteActionType._
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsString
import twentysix.playr.di.PlayRInfoConsumer
import play.api.mvc.InjectedController
import play.api.mvc.BaseController
import play.api.mvc.ControllerHelpers
import play.api.mvc.ControllerComponents
import play.api.mvc.Results
import play.api.mvc.ActionBuilder
import play.api.mvc.Request
import play.api.mvc.AnyContent

case class ApiInfoItem(path: String, label: String, actions: RestRouteActionType.ValueSet, children: Seq[ApiInfoItem])
object ApiInfoItem {
  implicit val jsonActionTypeWrites = new Writes[RestRouteActionType] {
    def writes(actionType: RestRouteActionType) = JsString(actionType.toString)
  }
  implicit val jsonWrites = Json.writes[ApiInfoItem]

  def fromRestRouteInfo(path: String, info: RestRouteInfo): ApiInfoItem = {
    ApiInfoItem(
      path,
      info.name,
      info.actions,
      info.subResources.map(i => fromRestRouteInfo(s"$path/${i.name}", i))
    )
  }
}

trait ApiInfo {
  this: RestRouter with BaseController =>

  def apiInfo = ApiInfo.action(Action)("", this)
}

object ApiInfo extends Results {
  def action(actionBuilder: ActionBuilder[Request, AnyContent])(prefix: String, router: RestRouter) = actionBuilder {
    request =>
      val info = router.routeResource
      val json = Json.toJson(ApiInfoItem.fromRestRouteInfo(info.name, info))
      if (request.queryString.contains("pretty")) {
        Ok(Json.prettyPrint(json))
      } else {
        Ok(json)
      }
  }

  def withController(controller: BaseController): PlayRInfoConsumer = new PlayRInfoConsumer {
    def apply(prefix: String, api: RestRouter): ConsumerRoutes = action(controller.Action)(prefix, api)
  }
}
