package twentysix.rest

import play.core.Router
import reflect.runtime.universe.Type

case class RestRouteInfo(path: String, resource: Resource, resourceType: Type, caps: ResourceCaps.ValueSet, subResources: Seq[RestRouteInfo])

trait RestRouter extends Router.Routes{
  def routeResources(path: String): Seq[RestRouteInfo]
}