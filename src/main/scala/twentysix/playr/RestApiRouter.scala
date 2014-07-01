package twentysix.playr

import scala.language.reflectiveCalls
import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import twentysix.playr.core.BaseResource

trait ApiRouter extends RestRouter with SimpleRouter{
  def routeMap: Map[String, RestRouter]
  def parentContext: Option[RouteFilterContext[_]]

  def routeResources(root: String): Seq[RestRouteInfo] = routeMap.flatMap{
    case (path, router) => router.routeResources(s"$root/$path")
  }.toSeq

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
}

case class RestApiRouter(routeMap: Map[String, RestRouter] = Map(), parentContext: Option[RouteFilterContext[_]] = None) extends ApiRouter {
  def add(t: (String, RestRouter)) = this.copy(routeMap=routeMap + t)
  def add(apiRouter: RestApiRouter) = this.copy(routeMap=routeMap ++ apiRouter.routeMap)
  def add[C<:BaseResource: ResourceWrapper](router: RestResourceRouter[C]): RestApiRouter = this.add(router.name -> router)
  def add[C<:BaseResource: ResourceWrapper](resource: C): RestApiRouter = this.add(resource.name -> new RestResourceRouter(resource))

  def :+(t: (String, RestRouter)) = this.add(t)
  def :+(apiRouter: RestApiRouter) = this.add(apiRouter)
  def :+[C<:BaseResource: ResourceWrapper](router: RestResourceRouter[C]) = this.add(router)
  def :+[C<:BaseResource: ResourceWrapper](resource: C) = this.add(resource)

  def withParentContext(context: RouteFilterContext[_]): RestApiRouter = this.copy(parentContext = Some(context))
}

object RestApiRouter {
  implicit def controller2Router[C<:BaseResource: ResourceWrapper](t: (String, C)) = RestApiRouter(Map(t._1 -> new RestResourceRouter[C](t._2)))
}
