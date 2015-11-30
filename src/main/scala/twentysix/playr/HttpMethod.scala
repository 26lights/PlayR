package twentysix.playr

import scala.language.implicitConversions
import scala.collection.breakOut

case class HttpMethod(name: String)

object GET     extends HttpMethod("GET")
object POST    extends HttpMethod("POST")
object DELETE  extends HttpMethod("DELETE")
object PUT     extends HttpMethod("PUT")
object PATCH   extends HttpMethod("PATCH")
object OPTIONS extends HttpMethod("OPTIONS")

object HttpMethod {
  val All: Map[String, HttpMethod] = Seq(GET, POST, DELETE, PUT, PATCH, OPTIONS).map( m => m.name -> m )(breakOut)
}
