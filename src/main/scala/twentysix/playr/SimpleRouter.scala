package twentysix.playr

import play.api.routing.Router
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler

trait RouterWithPrefix extends Router { self =>

  def documentation: Seq[(String, String, String)] = Seq.empty

  def routesWithPrefix(prefix: String) = self.routes

  def withPrefix(prefix: String): Router = {
    if ((prefix=="") || (prefix == "/")) {
      self
    } else {
      new Router {
        def routes = {
          val p = if (prefix.endsWith("/")) prefix.drop(1) else prefix
          val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
            case rh: RequestHeader if rh.path.startsWith(p) => {
              rh.copy(path = rh.path.drop(p.length))
            }
          }
          Function.unlift(prefixed.lift.andThen(_.flatMap(self.routesWithPrefix(p).lift)))
        }

        def withPrefix(prefix: String) = self.withPrefix(prefix)
        def documentation = self.documentation
      }
    }
  }
}

trait SimpleRouter extends RouterWithPrefix {
  def routeRequest(header: RequestHeader, path: String, method: String): Option[Handler]

  def routes = Function.unlift { requestHeader =>
    routeRequest(requestHeader, requestHeader.path, requestHeader.method)
  }
}
