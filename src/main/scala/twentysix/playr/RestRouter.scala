package twentysix.playr

import play.core.Router
import reflect.runtime.universe.Type

trait RestRouteInfo{
  val name: String
  val resourceType: Type
  val caps: ResourceCaps.ValueSet
  val subResources: Seq[RestRouteInfo]
  val actions: RestRouteActionType.ValueSet = {
    RestRouteActionType.ValueSet( caps.flatMap {
      case ResourceCaps.Read => Seq(RestRouteActionType.Read, RestRouteActionType.List)
      case ResourceCaps.Write => Some(RestRouteActionType.Write)
      case ResourceCaps.Update => Some(RestRouteActionType.Update)
      case ResourceCaps.Delete => Some(RestRouteActionType.Delete)
      case ResourceCaps.Create => Some(RestRouteActionType.Create)
      case ResourceCaps.Action => Some(RestRouteActionType.Custom)
      case ResourceCaps.Parent => Some(RestRouteActionType.Traverse)
      case ResourceCaps.Api => Some(RestRouteActionType.Traverse)
      case _ => None
    }.toSeq :_*)
  }
}

case class ApiRestRouteInfo(name: String, resourceType: Type, caps: ResourceCaps.ValueSet, subResources: Seq[RestRouteInfo]) extends RestRouteInfo
case class ActionRestRouteInfo(name: String, resourceType: Type, caps: ResourceCaps.ValueSet, subResources: Seq[RestRouteInfo], method: HttpMethod) extends RestRouteInfo

trait RestRouter extends Router.Routes{
  val name: String
  def routeResource: RestRouteInfo
  def withParentContext(context: RouteFilterContext[_]): RestRouter
}
