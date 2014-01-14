package twentysix.rest

import play.core.Router

case class RestRouteInfo(val path: String, val resource: Resource, val subResources: Seq[RestRouteInfo])

trait RestRouter extends Router.Routes{
  def routeResources(path: String): Seq[RestRouteInfo]
}