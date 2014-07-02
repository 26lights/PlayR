package twentysix.playr

import scala.language.reflectiveCalls
import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import twentysix.playr.core.BaseResource

case class RestApiRouter(val name: String, routeMap: Map[String, RestRouter] = Map(), parentContext: Option[RouteFilterContext[_]] = None) extends RestRouter with SimpleRouter {

  def routeResource: RestRouteInfo = {
    val subResources = routeMap.map {
      case (path, router) => router.routeResource
    }.toSeq

    ApiRestRouteInfo(name, typeOf[RestApiRouter], ResourceCaps.ValueSet(ResourceCaps.Api), subResources)
  }

  private val SubPathExpression = "^(/([^/]+)).*$".r

  def routeRequest(requestHeader: RequestHeader, path: String, method: String): Option[Handler] = {
    path match {
      case SubPathExpression(subPrefix, subPath) => {
        routeMap.get(subPath).flatMap{ router =>
          Router.Include {
            val subRouter = router.withParentContext(RouteFilterContext(subPath, None, None, parentContext))
            subRouter.setPrefix(prefix+subPrefix)
            subRouter
          }.unapply(requestHeader)
        }
      }
      case _ => None
    }
  }


  def add(router: RestRouter) = this.copy(routeMap=routeMap + (router.name -> router))
  def addRoutes(apiRouter: RestApiRouter) = this.copy(routeMap=routeMap ++ apiRouter.routeMap)
  def add[C<:BaseResource: ResourceWrapper](resource: C): RestApiRouter = this.add(new RestResourceRouter(resource))

  def :+(router: RestRouter) = this.add(router)
  def :++(apiRouter: RestApiRouter) = this.addRoutes(apiRouter)
  def :+[C<:BaseResource: ResourceWrapper](resource: C) = this.add(resource)

  def withParentContext(context: RouteFilterContext[_]): RestApiRouter = this.copy(parentContext = Some(context))
}

object RootApiRouter {
  def apply() = RestApiRouter("")
}

object RestApiRouter {
  implicit def controller2Router[C<:BaseResource: ResourceWrapper](t: (String, C)): RestRouter = new RestResourceRouter[C](t._2, path=Some(t._1))
}
