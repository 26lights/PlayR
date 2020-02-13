package controllers

import twentysix.playr.RestRouter
import play.api.mvc.BaseController
import play.api.mvc.Results
import twentysix.playr.RestRouteActionType
import twentysix.playr.RestRouteInfo
import twentysix.playr.ResourceCaps
import twentysix.playr.di.PlayRInfoConsumer

case class JQueryApiItem(
    path: String,
    name: String,
    actions: RestRouteActionType.ValueSet,
    children: Seq[JQueryApiItem]
)

object JQueryApiItem {
  protected val CapsFilter = ResourceCaps.ValueSet(
    ResourceCaps.Read,
    ResourceCaps.Write,
    ResourceCaps.Create,
    ResourceCaps.Update,
    ResourceCaps.Delete
  )

  def fromRouteRouteInfo(api: RestRouteInfo, path: String = ""): Seq[JQueryApiItem] = {
    val name = api.name.capitalize
    if (CapsFilter.intersect(api.caps).isEmpty) {
      api.subResources.flatMap { subApi =>
        fromRouteRouteInfo(subApi, path + api.name + "/")
      }
    } else {
      Seq(JQueryApiItem(path + api.name, name, api.actions, children = api.subResources.flatMap { subApi =>
        fromRouteRouteInfo(subApi)
      }))
    }
  }
}

object JQueryApi extends Results {
  def withController(controller: BaseController) = new PlayRInfoConsumer {
    def apply(prefix: String, api: RestRouter) = controller.Action { request =>
      Ok(views.js.jquery(JQueryApiItem.fromRouteRouteInfo(api.routeResource), prefix))
    }
  }
}
