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
  val Read, Write, Create, Delete, Update, Parent, Child, Action = Value
}

/**
 * Define the conversion from an url id to a real object
 */
trait BaseResource extends Controller{
  def name: String
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

trait Resource[R] extends BaseResource {
  type ResourceType = R
}


/**
 * Respond to HTTP GET method
 */
trait BaseResourceRead extends BaseResource {

  def read(id: ResourceType): EssentialAction
  def list(): EssentialAction

  def readRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceRead[R] extends BaseResourceRead with Resource[R]

/**
 * Respond to HTTP PUT method
 */
trait BaseResourceWrite extends BaseResource{
  def write(id: ResourceType): EssentialAction

  def writeRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceWrite[R] extends BaseResourceWrite with Resource[R]

/**
 * Respond to HTTP POST method
 */
trait BaseResourceCreate extends BaseResource{
  def create(): EssentialAction
}
trait ResourceCreate[R] extends BaseResourceCreate with Resource[R]

/**
 * Respond to HTTP DELETE method
 */
trait BaseResourceDelete extends BaseResource{
  def delete(id: ResourceType): EssentialAction

  def deleteRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceDelete[R] extends BaseResourceDelete with Resource[R]


/**
 * Respond to HTTP PATCH method
 */
trait BaseResourceUpdate extends BaseResource{
  def update(id: ResourceType): EssentialAction

  def updateRequestWrapper(sid: String, block: (String => Option[EssentialAction])): Option[EssentialAction] = requestWrapper(sid, block)
}
trait ResourceUpdate[R] extends BaseResourceUpdate with Resource[R]



//-------------------------
//---- Shortcut traits ----
//-------------------------

trait RestReadController[R] extends Resource[R]
                               with ResourceRead[R]

/**
 * Read and write controller: implements GET, POST and PATCH for partial updates
 */
trait RestRwController[R] extends Resource[R]
                             with ResourceCreate[R]
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
trait RestCrudController[R] extends Resource[R]
                               with ResourceCreate[R]
                               with ResourceRead[R]
                               with ResourceDelete[R]
                               with ResourceWrite[R]
