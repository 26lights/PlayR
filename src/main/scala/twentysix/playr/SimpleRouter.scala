package twentysix.playr

import play.api.routing.Router
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.Logger

trait PrefixAware {
  val routerPrefix: String
}

trait SimpleRouter extends Router with PrefixAware{ self =>
  val routerPrefix = ""

  def routeRequest(header: RequestHeader, path: String, method: String): Option[Handler]

  def documentation: Seq[(String, String, String)] = Seq.empty

  def withPrefix(prefix: String): Router = {
    if ((prefix=="") || (prefix == "/")) {
      self
    } else {
      val p = if (prefix.endsWith("/")) prefix.drop(1) else prefix
      new Router with PrefixAware{
        def routes = {
          val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
            case rh: RequestHeader if rh.path.startsWith(p) => rh.copy(path = rh.path.drop(p.length))
          }
          Function.unlift(prefixed.lift.andThen(_.flatMap(self.routes.lift)))
        }
        val routerPrefix = p

        def withPrefix(prefix: String) = self.withPrefix(prefix)
        def documentation = self.documentation
      }
    }
  }

  def routes = Function.unlift { requestHeader =>
    Logger.debug(s"request: ${requestHeader.path}")
    routeRequest(requestHeader, requestHeader.path, requestHeader.method)
  }
}
