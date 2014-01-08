package twentysix.rest

import play.core.Router
import play.api.mvc.EssentialAction
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Controller
import play.api.Logger

object ResourceCaps extends Enumeration {
  type ResourceCaps = Value
  val Identity, Read, Write, Create, Delete, Update, Parent, Child, Action = Value
}

trait Resource{
  var caps = ResourceCaps.ValueSet.empty
  def name: String
}

case class ResourceAction(name: String, method: String) extends Resource {
  caps += ResourceCaps.Action
}

/**
 * Define the conversion from an url id to a real object
 */
trait IdentifiedResource[R] extends Resource{
  caps += ResourceCaps.Identity

  def toNumber[N](id: String, f: String => N): Option[N] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt f(id)
  }

  def toInt(id: String) = toNumber(id, _.toInt)
  def toLong(id: String) = toNumber(id, _.toLong)

  def fromId(id: String): Option[R]

  def requestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = block(sid)
}

/**
 * Respond to HTTP GET method
 */
trait ResourceRead[R] extends IdentifiedResource[R] {
  caps+=ResourceCaps.Read

  def read(id: R): EssentialAction
  def list(): EssentialAction

  def readRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}

/**
 * Respond to HTTP PUT method
 */
trait ResourceWrite[R] extends IdentifiedResource[R]{
  caps+=ResourceCaps.Write

  def write(id: R): EssentialAction

  def writeRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate extends Resource{
  caps+=ResourceCaps.Create

  def create(): EssentialAction
}

/**
 * Respond to HTTP DELETE method
 */
trait ResourceDelete[R] extends IdentifiedResource[R]{
  caps+=ResourceCaps.Delete

  def delete(id: R): EssentialAction

  def deleteRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}

/**
 * Respond to HTTP PATCH method
 */
trait ResourceUpdate[R] extends IdentifiedResource[R]{
  caps+=ResourceCaps.Update

  def update(id: R): EssentialAction

  def updateRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}


/**
 * Define link to other resources accessible via a sub paths
 */
trait ResourceRoutes[R] extends IdentifiedResource[R] {
  caps+=ResourceCaps.Parent

  def RouteMap = ResourceRouteMap[R]()
  val routeMap: ResourceRouteMap[R]
}

/**
 * Can create new instances tailored for a specific parent reosurce
 */
trait SubResource[P, S<:SubResource[P, S]] extends Resource {
  self: S =>
  caps+=ResourceCaps.Child

  def withParent(parentResource: P): S
}



//-------------------------
//---- Shortcut traits ----
//-------------------------

trait RestController[R] extends Controller
                           with IdentifiedResource[R]

trait RestReadController[R] extends Controller
                               with IdentifiedResource[R]
                               with ResourceRead[R]

/**
 * Read and write controller: implements GET, POST and PATCH for partial updates
 */
trait RestRwController[R] extends Controller
                             with IdentifiedResource[R]
                             with ResourceCreate
                             with ResourceRead[R]
                             with ResourceUpdate[R]

/**
 * Same as RestRWController plus DELETE method
 */
trait RestRwdController[R] extends RestRwController[R]
                              with ResourceDelete[R]

/**
 * Classic rest controller: handle GET, POST, PUT and DELETE http methods
 */
trait RestCrudController[R] extends Controller
                               with IdentifiedResource[R]
                               with ResourceCreate
                               with ResourceRead[R]
                               with ResourceDelete[R]
                               with ResourceWrite[R]
