package twentysix.playr

import scala.language.implicitConversions
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.EssentialAction
import play.api.mvc.Results.Ok
import play.api.mvc.Action
import play.api.routing.Router

/**
 * Helper traits to help creating dependency injectable play'r routers
 */
object di {
  trait PlayRRouter extends RouterWithPrefix{
    val api: RestApiRouter

    def routes = api.routes
  }

  trait PlayRInfoConsumer {
    trait ConsumerRoutes {
      def toRoutes(path: String): Router.Routes
    }
    object ConsumerRoutes {
      implicit def fromAction(action: EssentialAction) = new ConsumerRoutes {
        def toRoutes(path: String) = { case rh if ((rh.path == "/" + path) && (rh.method=="GET")) => action }
      }
      implicit def fromRouter(router: Router) = new ConsumerRoutes {
        def toRoutes(path: String) = router.withPrefix("/" + path).routes
      }
    }

    def apply(prefix: String, api: RestRouter): ConsumerRoutes
  }

  trait PlayRInfo { self: PlayRRouter =>

    val info: Map[String, PlayRInfoConsumer]

    override def routesWithPrefix(prefix: String) = api.routes orElse info.foldLeft(PartialFunction.empty[RequestHeader, Handler]) {
      (router, entry) => {
        val path = entry._1
        val handler = entry._2(prefix, api)
        router orElse handler.toRoutes(path)
      }
    } orElse PartialFunction[RequestHeader, Handler]{
      case rh: RequestHeader => Action(r => Ok(s"not found $rh (${rh.method} ${rh.path})"))
    }

    override def routes = routesWithPrefix("")
  }

}
