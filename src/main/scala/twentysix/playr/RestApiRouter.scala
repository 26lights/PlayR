package twentysix.playr

import play.core.routing.Include
import scala.language.reflectiveCalls
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import twentysix.playr.core.BaseResource
import twentysix.playr.RestRouteActionType.Traverse

case class RestApiRouter(name: String, routeMap: Map[String, RestRouter] = Map(), parentContext: Option[RouteFilterContext[_]] = None, filter: Option[RestRouterFilter] = None) extends RestRouter with SimpleRouter {
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
          val context = RouteFilterContext(name, None, None, parentContext)
          ApplyRouterFilter(filter, Traverse, context.contextPath, requestHeader) { () =>
            Include {
              router.withParentContext(context, filter)
                    .withPrefix(subPrefix)
            }.unapply(requestHeader)
          }
        }
      }
      case _ => None
    }
  }


  def add(router: RestRouter) = this.copy(routeMap=routeMap + (router.name -> router))
  def addRoutes(apiRouter: RestApiRouter) = this.copy(routeMap=routeMap ++ apiRouter.routeMap)
  def add[C<:BaseResource: ResourceWrapper](resource: C): RestApiRouter = this.add(new RestResourceRouter[C](resource))
  def add[C<:BaseResource: ResourceWrapper](t: (String, C)): RestApiRouter = this.add(new RestResourceRouter[C](t._2, path=Some(t._1)))
  def addResource[C<:BaseResource: ResourceWrapper](resource: C)(block: RestResourceRouter[C]=>RestResourceRouter[C]): RestApiRouter =
    this.add(block(new RestResourceRouter(resource)))
  def addResource[C<:BaseResource: ResourceWrapper](t: (String, C))(block: RestResourceRouter[C]=>RestResourceRouter[C]): RestApiRouter =
    this.add(block(new RestResourceRouter[C](t._2, path=Some(t._1))))

  def :+(router: RestRouter) = this.add(router)
  def ++(apiRouter: RestApiRouter) = this.addRoutes(apiRouter)
  def :+[C<:BaseResource: ResourceWrapper](resource: C) = this.add(resource)
  def :+[C<:BaseResource: ResourceWrapper](t: (String, C)): RestApiRouter = this.add(t)
  def withFilter(f: RestRouterFilter) = this.copy(filter=Some(f))

  def withParentContext(context: RouteFilterContext[_], parentFilter: Option[RestRouterFilter]): RestApiRouter =
    this.copy(parentContext = Some(context), filter=this.filter orElse parentFilter)
}

object RootApiRouter {
  def apply() = RestApiRouter("")
}
