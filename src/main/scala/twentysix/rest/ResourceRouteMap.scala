import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import twentysix.rest.RestRouteInfo
import play.api.mvc.Controller
import twentysix.rest.Resource
import twentysix.rest.RestResourceRouter
import play.core.Router
import twentysix.rest.SubResource
import twentysix.rest.ResourceWrapperGenerator
import play.api.mvc.EssentialAction
import twentysix.rest.ResourceAction
import play.api.mvc.Action
import play.api.mvc.Results

case class ResourceRouteMap[R](routeMap: Map[String, ResourceRouteMap[R]#Routing] = Map[String, ResourceRouteMap[R]#Routing]()) {
  sealed trait Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler]
    def routeInfo(path: String): RestRouteInfo
  }

  class ResourceRouting[C<:Controller with Resource](val router: RestResourceRouter[C]) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.resourceWrapper.routeResources(path)
  }

  class ControllerRouting[C<:Controller with SubResource[R, C]](val controller: C) extends Routing{
    val resourceWrapperGenerator = new ResourceWrapperGenerator(controller)
    val resourceWrapper = resourceWrapperGenerator.forController(controller)
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestResourceRouter(resourceWrapperGenerator.forController(controller.withParent(id)))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = resourceWrapper.routeResources(path)
  }

  class ActionRouting(val method: String, val f: R => EssentialAction) extends Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
    def routeInfo(path: String) = RestRouteInfo(path, ResourceAction(path, method), Seq())
  }


  def add(t: (String, ResourceRouteMap[R]#Routing)) = this.copy(routeMap = this.routeMap + t )

  def add[C<:Controller with Resource](route: String, router: RestResourceRouter[C]): ResourceRouteMap[R] =
    this.add(route-> new ResourceRouting(router))
  def add[C<:Controller with SubResource[R, C]](route: String, controller: C): ResourceRouteMap[R] =
    this.add(route-> new ControllerRouting(controller))
  def add(route: String, method: String, f: (R => EssentialAction)): ResourceRouteMap[R] =
    this.add(route-> new ActionRouting(method, f))

}

