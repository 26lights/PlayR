package twentysix.rest

import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import play.api.Logger

class RestApiRouter(val routeMap: Map[String, RestRouter]) extends RestRouter{
  protected var _prefix: String =""

  def routeResources = routeMap.flatMap {
    case (path, router) => router.routeResources.map {
      case(subPath, resource) => (s"$prefix/${path}${subPath}" -> resource) 
    }
  }

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

object RestApiRouter{
  def apply(elems: (String, RestRouter)*) = new RestApiRouter(Map(elems: _*))
}
