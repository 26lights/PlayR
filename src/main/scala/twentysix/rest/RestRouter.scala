package twentysix.rest

import play.core.Router

trait RestRouter extends Router.Routes{
  def routeResources: Map[String, Resource]
}