package twentysix.rest

import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import play.api.Logger

trait ApiRouter extends RestRouter{
  def routeMap: Map[String, RestRouter]

  protected var _prefix: String =""

  def routeResources(root: String) = routeMap.flatMap{
    case (path, router) => router.routeResources(s"$root/$path")
  }.toSeq

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private val SubPathExpression = "^(/([^/]+)).*$".r

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())

        path match {
          case SubPathExpression(subPrefix, subPath) => {
            routeMap.get(subPath).flatMap{ router =>
              Router.Include {
                router.setPrefix(prefix+subPrefix)
                router
              }.unapply(requestHeader)
            }.getOrElse(default(requestHeader))
          }
          case _ => default(requestHeader)
        }
      } else {
        default(requestHeader)
      }
    }

    def isDefinedAt(requestHeader: RequestHeader): Boolean = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())

        path match {
          case SubPathExpression(subPath) if routeMap contains subPath => true
          case _ => false
        }
      } else {
        false
      }
    }
  }
}

case class RestApiRouter(routeMap: Map[String, RestRouter] = Map()) extends ApiRouter {
  def add(t: (String, RestRouter)) = this.copy(routeMap=routeMap + t)
  def add(apiRouter: RestApiRouter) = this.copy(routeMap=routeMap ++ apiRouter.routeMap)
  def add(resource: Controller with Resource): RestApiRouter = this.add(resource.name -> RestResourceRouter(resource))

  def :+(t: (String, RestRouter)) = this.add(t)
  def :+(apiRouter: RestApiRouter) = this.add(apiRouter)
  def :+(resource: Controller with Resource) = this.add(resource)
}
object RestApiRouter {
  implicit def controller2Router(t: (String, Controller with Resource)) = RestApiRouter(Map(t._1 -> RestResourceRouter(t._2)))
}
