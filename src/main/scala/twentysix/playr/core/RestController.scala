package twentysix.playr.core

import play.api.mvc.EssentialAction
import play.api.mvc.Controller

/**
 * Define the conversion from an url id to a real object
 */
trait BaseResource extends Controller {
  def name: String
  type IdentifierType
  type ResourceType

  // String conversion helper methods

  def toNumber[N](id: String, f: String => N): Option[N] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt f(id)
  }

  def toInt(id: String) = toNumber(id, _.toInt)
  def toLong(id: String) = toNumber(id, _.toLong)

  def parseId(sid: String): Option[IdentifierType]

  // Implementors should define a variant of the following methods:
  // def fromId(id: IdentifierType): Option[ResourceType]
}

trait Resource[I, R] extends BaseResource {
  type IdentifierType = I
  type ResourceType = R
}


/**
 * Respond to HTTP GET method
 */
trait ResourceRead {
  this: BaseResource =>

  def readResource(id: IdentifierType): Option[EssentialAction]
  def listResource: EssentialAction

  // Implementors should define a variant of the following methods:
  // def read(resource: ResourceType): EssentialAction
  // def list: EssentialAction
}

/**
 * Respond to HTTP PUT method
 */
trait ResourceWrite {
  this: BaseResource =>

  def writeResource(id: IdentifierType): Option[EssentialAction]

  // Implementors should define a variant of the following methods:
  // def write(resource: ResourceType): EssentialAction
}

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate {
  this: BaseResource =>

  def createResource: EssentialAction

  // Implementors should define a variant of the following methods:
  // def create: EssentialAction
}

/**
 * Respond to HTTP DELETE method
 */
trait ResourceDelete {
  this: BaseResource =>

  def deleteResource(id: IdentifierType): Option[EssentialAction]

  // Implementors should define a variant of the following methods:
  // def delete(resource: ResourceType): EssentialAction
}


/**
 * Respond to HTTP PATCH method
 */
trait ResourceUpdate {
  this: BaseResource =>

  def updateResource(id: IdentifierType): Option[EssentialAction]

  // Implementors should define a variant of the following methods:
  // def update(resource: ResourceType): EssentialAction
}
