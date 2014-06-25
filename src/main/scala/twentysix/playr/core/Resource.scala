package twentysix.playr.core

import reflect.runtime.universe.Type
import play.api.mvc.EssentialAction
import play.api.mvc.Controller
import scala.util.control.Exception.catching
import twentysix.playr.ResourceWrapper

/**
 * Define the conversion from an url id to a real object
 */
trait BaseResource extends Controller {
  def name: String
  type IdentifierType

  def toNumber[N](id: String, f: String => N): Option[N] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt f(id)
  }

  def toInt(id: String) = toNumber(id, _.toInt)
  def toLong(id: String) = toNumber(id, _.toLong)

  def parseId(id: String): Option[IdentifierType]
}

trait ResourceTrait[R] extends BaseResource {
  type IdentifierType = R
}

/**
 * Respond to HTTP GET method
 */
trait ResourceRead {
  this: BaseResource =>

  def readResource(id: IdentifierType): Option[EssentialAction]
  def listResource: Option[EssentialAction]
}

/**
 * Respond to HTTP PUT method
 */
trait ResourceWrite {
  this: BaseResource =>

  def writeResource(id: IdentifierType): Option[EssentialAction]
}

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate {
  this: BaseResource =>

  def createResource: Option[EssentialAction]
}

/**
 * Respond to HTTP DELETE method
 */
trait ResourceDelete {
  this: BaseResource =>

  def deleteResource(id: IdentifierType): Option[EssentialAction]
}


/**
 * Respond to HTTP PATCH method
 */
trait ResourceUpdate {
  this: BaseResource =>

  def updateResource(id: IdentifierType): Option[EssentialAction]
}

/**
 * Handle custom sub paths associated to a resource
 */
abstract class ResourceAction[C<:BaseResource] {
  def handleAction(controller: C, id: C#IdentifierType): Option[EssentialAction]
}

/**
 * Handle sub resource controller construction
 */
abstract class ControllerFactory[P<:BaseResource, C<:BaseResource: ResourceWrapper] {
  def construct(parent: P, id: P#IdentifierType): C
}
