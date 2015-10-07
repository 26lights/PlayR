package twentysix.playr

import play.api.routing.Router
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler

trait SimpleRouter extends Router{
  protected var _prefix: String =""

  def withPrefix(newPrefix: String): SimpleRouter = {
    _prefix = newPrefix
    this
  }

  def prefix = _prefix
  def documentation = Nil

  def routeRequest(header: RequestHeader, path: String, method: String): Option[Handler]
  
  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        val method = requestHeader.method
        routeRequest(requestHeader, path, method).getOrElse(default(requestHeader))
      } else {
        default(requestHeader)
      }
    }

    def isDefinedAt(requestHeader: RequestHeader): Boolean = {
      requestHeader.path.startsWith(_prefix)
    }
  }
}
