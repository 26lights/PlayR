package twentysix.rest

import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import play.api.Logger

trait ApiRouter extends RestRouter with SimpleRouter{
  def routeMap: Map[String, RestRouter]

  def routeResources(root: String) = routeMap.flatMap{
    case (path, router) => router.routeResources(s"$root/$path")
  }.toSeq

  private val SubPathExpression = "^(/([^/]+)).*$".r

  def routeRequest(requestHeader: RequestHeader, path: String, method: String) = {
    path match {
      case SubPathExpression(subPrefix, subPath) => {
        routeMap.get(subPath).flatMap{ router =>
          Router.Include {
            router.setPrefix(prefix+subPrefix)
            router
          }.unapply(requestHeader)
        }
      }
      case _ => None
    }
  }
}

case class RestApiRouter(routeMap: Map[String, RestRouter] = Map()) extends ApiRouter {
  def add(t: (String, RestRouter)) = this.copy(routeMap=routeMap + t)
  def add(apiRouter: RestApiRouter) = this.copy(routeMap=routeMap ++ apiRouter.routeMap)
  def add(resource: Controller with Resource): RestApiRouter = this.add(resource.name -> new RestResourceRouter(resource))

  def :+(t: (String, RestRouter)) = this.add(t)
  def :+(apiRouter: RestApiRouter) = this.add(apiRouter)
  def :+(resource: Controller with Resource) = this.add(resource)
}
object RestApiRouter {
  implicit def controller2Router(t: (String, Controller with Resource)) = RestApiRouter(Map(t._1 -> new RestResourceRouter(t._2)))
}
