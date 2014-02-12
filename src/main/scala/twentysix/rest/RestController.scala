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
  def name: String
}

/**
 * Define the conversion from an url id to a real object
 */
trait BaseIdentifiedResource extends Resource{
  type ResourceType

  def toNumber[N](id: String, f: String => N): Option[N] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt f(id)
  }

  def toInt(id: String) = toNumber(id, _.toInt)
  def toLong(id: String) = toNumber(id, _.toLong)

  def fromId(id: String): Option[ResourceType]

  def requestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = block(sid)
}
trait IdentifiedResource[R] extends BaseIdentifiedResource {
  type ResourceType = R
}


/**
 * Respond to HTTP GET method
 */
trait BaseResourceRead extends BaseIdentifiedResource {

  def read(id: ResourceType): EssentialAction
  def list(): EssentialAction

  def readRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceRead[R] extends BaseResourceRead with IdentifiedResource[R]

/**
 * Respond to HTTP PUT method
 */
trait BaseResourceWrite extends BaseIdentifiedResource{
  def write(id: ResourceType): EssentialAction

  def writeRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceWrite[R] extends BaseResourceWrite with IdentifiedResource[R]

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate extends Resource{
  def create(): EssentialAction
}

/**
 * Respond to HTTP DELETE method
 */
trait BaseResourceDelete extends BaseIdentifiedResource{
  def delete(id: ResourceType): EssentialAction

  def deleteRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceDelete[R] extends BaseResourceDelete with IdentifiedResource[R]


/**
 * Respond to HTTP PATCH method
 */
trait BaseResourceUpdate extends BaseIdentifiedResource{
  def update(id: ResourceType): EssentialAction

  def updateRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceUpdate[R] extends BaseResourceUpdate with IdentifiedResource[R]



/**
 * Define link to other resources accessible via a sub paths
 */
trait BaseResourceRoutes extends BaseIdentifiedResource {
  def RouteMap = ResourceRouteMap[ResourceType]()
  val routeMap: ResourceRouteMap[ResourceType]
}
trait ResourceRoutes[R] extends BaseResourceRoutes with IdentifiedResource[R]

/**
 * Can create new instances tailored for a specific parent resource
 */
trait SubResource[P, S<:SubResource[P, S]] extends Resource {
  self: S =>
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
