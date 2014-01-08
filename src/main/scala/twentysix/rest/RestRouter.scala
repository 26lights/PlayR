package twentysix.rest

import play.core.Router

case class RestRouteInfo(path: String, resource: Resource, subResources: Seq[RestRouteInfo])

trait RestRouter extends Router.Routes{
  def routeResources(path: String): Seq[RestRouteInfo]
}