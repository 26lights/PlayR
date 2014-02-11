package twentysix.rest

import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Controller
import play.core.Router
import play.api.mvc.EssentialAction
import play.api.mvc.Action
import play.api.mvc.Results
import play.api.Logger

case class ResourceRouteMap[R](routeMap: Map[String, ResourceRouteMap[R]#Routing] = Map[String, ResourceRouteMap[R]#Routing]()) {
  sealed trait Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler]
    def routeInfo(path: String): RestRouteInfo
  }

  class SubResourceRouting(val router: SubRestResourceRouter[R, _]) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val subRouter = router.withParent(id)
        subRouter.setPrefix(prefix)
        subRouter
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }

  class ResourceRouting(val router: RestResourceRouter[_]) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }

  class ActionRouting(val method: String, val f: R => EssentialAction) extends Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
    def routeInfo(path: String) = RestRouteInfo(path, ResourceAction(path, method), Seq())
  }


  def add(t: (String, ResourceRouteMap[R]#Routing)) = this.copy(routeMap = this.routeMap + t )

  def add(route: String, router: SubRestResourceRouter[R, _]): ResourceRouteMap[R] =
    this.add(route-> new SubResourceRouting(router))
  def add(route: String, router: RestResourceRouter[_]): ResourceRouteMap[R] =
    this.add(route-> new ResourceRouting(router))
  def add(route: String, method: String, f: (R => EssentialAction)): ResourceRouteMap[R] =
    this.add(route-> new ActionRouting(method, f))

}

