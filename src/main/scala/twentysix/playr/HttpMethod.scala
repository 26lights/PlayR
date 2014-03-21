package twentysix.playr

import scala.language.implicitConversions

case class HttpMethod(name: String)

object GET     extends HttpMethod("GET")
object POST    extends HttpMethod("POST")
object DELETE  extends HttpMethod("DELETE")
object PUT     extends HttpMethod("PUT")
object PATCH   extends HttpMethod("PATCH")
object OPTIONS extends HttpMethod("OPTIONS")


