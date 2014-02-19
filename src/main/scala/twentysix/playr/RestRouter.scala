package twentysix.rest

import play.core.Router
import reflect.runtime.universe.Type

trait RestRouteInfo{
  val path: String
  val name: String
  val resourceType: Type
  val caps: ResourceCaps.ValueSet
  val subResources: Seq[RestRouteInfo]
}
case class ApiRestRouteInfo(path: String, name: String, resourceType: Type, caps: ResourceCaps.ValueSet, subResources: Seq[RestRouteInfo]) extends RestRouteInfo
case class ActionRestRouteInfo(path: String, name: String, resourceType: Type, caps: ResourceCaps.ValueSet, subResources: Seq[RestRouteInfo], method: String) extends RestRouteInfo

trait RestRouter extends Router.Routes{
  def routeResources(path: String): Seq[RestRouteInfo]
}