package twentysix.rest

import play.core.Router
import play.api.mvc.EssentialAction
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Controller
import play.api.Logger

/**
 * Define the conversion from an url id to a real object
 */
trait IdentifiedResource[Id] {
  def toNumber[N](id: String, f: String => N): Option[N] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt f(id)
  }

  def toInt(id: String) = toNumber(id, _.toInt)
  def toLong(id: String) = toNumber(id, _.toLong)

  def fromId(id: String): Option[Id]

}

/**
 * Respond to HTTP GET method
 */
trait ResourceRead[Id] extends IdentifiedResource[Id]{
  def get(id: Id): EssentialAction
  def list(): EssentialAction
}

/**
 * Respond to HTTP PUT method
 */
trait ResourceWrite[Id] extends IdentifiedResource[Id]{
  def write(id: Id): EssentialAction
}

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate {
  def create(): EssentialAction
}

/**
 * Respond to HTTP DELETE method
 */
trait ResourceDelete[Id] extends IdentifiedResource[Id]{
  def delete(id: Id): EssentialAction
}

/**
 * Respond to HTTP PATCH method
 */
trait ResourceUpdate[Id] extends IdentifiedResource[Id]{
  def update(id: Id): EssentialAction
}

/**
 * Define link to other resources accessible via a sub paths
 */
trait SubResource[Id] extends IdentifiedResource[Id]{
  def subResources: Map[String, RestPath[Id]]
}

trait RestController[Id] extends Controller
                             with IdentifiedResource[Id]

trait RestReadController[Id] extends Controller
                                 with IdentifiedResource[Id]
                                 with ResourceRead[Id]

/**
 * Read and write controller: implements GET, POST and PATCH for partial updates
 */
trait RestRwController[Id] extends Controller
                               with IdentifiedResource[Id]
                               with ResourceCreate
                               with ResourceRead[Id]
                               with ResourceUpdate[Id]

/**
 * Same as RestRWController plus DELETE method
 */
trait RestRwdController[Id] extends RestRwController[Id]
                                with ResourceDelete[Id]

/**
 * Classic rest controller: handle GET, POST, PUT and DELETE http methods
 */
trait RestCrudController[Id] extends Controller
                                 with IdentifiedResource[Id]
                                 with ResourceCreate
                                 with ResourceRead[Id]
                                 with ResourceDelete[Id]
                                 with ResourceWrite[Id]
