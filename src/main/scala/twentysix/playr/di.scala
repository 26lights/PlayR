package twentysix.playr

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
    def apply(prefix: String, api: RestRouter): EssentialAction
  }

  trait PlayRInfo { self: PlayRRouter =>

    val info: Map[String, PlayRInfoConsumer]

    override def routesWithPrefix(prefix: String) = api.routes orElse info.foldLeft(PartialFunction.empty[RequestHeader, Handler]) {
      (router, entry) => {
        val path = entry._1
        val handler = entry._2(prefix, api)
        router orElse {
          case rh if ((rh.path == "/"+ path) && (rh.method=="GET")) => handler
        }
      }
    }

    override def routes = routesWithPrefix("")
  }

}
