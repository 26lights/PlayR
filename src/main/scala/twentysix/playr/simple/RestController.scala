package twentysix.playr.simple

import play.api.mvc.EssentialAction
import play.api.mvc.Controller
import twentysix.playr.core

trait BaseResource extends core.BaseResource{
  type IdentifierType = ResourceType

  def fromId(id: IdentifierType): Option[ResourceType] = Some(id)
}

trait Resource[R] extends BaseResource {
  type ResourceType = R
}


/**
 * Respond to HTTP GET method
 */
trait ResourceRead extends core.ResourceRead{
  this: BaseResource =>

  def read(resource: ResourceType): EssentialAction
  def list: EssentialAction

  def readResource(id: IdentifierType) = fromId(id).map(read)
  def listResource = list
}

/**
 * Respond to HTTP PUT method
 */
trait ResourceWrite extends core.ResourceWrite{
  this: BaseResource =>

  def write(id: ResourceType): EssentialAction

  def writeResource(id: IdentifierType) = fromId(id).map(write)
}

/**
 * Respond to HTTP POST method
 */
trait ResourceCreate extends core.ResourceCreate {
  this: BaseResource =>

  def create: EssentialAction

  def createResource = create
}

/**
 * Respond to HTTP DELETE method
 */
trait ResourceDelete extends core.ResourceDelete{
  this: BaseResource =>

  def delete(id: ResourceType): EssentialAction

  def deleteResource(id: IdentifierType) = fromId(id).map(delete)
}


/**
 * Respond to HTTP PATCH method
 */
trait ResourceUpdate extends core.ResourceUpdate{
  this: BaseResource =>

  def update(id: ResourceType): EssentialAction

  def updateResource(id: IdentifierType) = fromId(id).map(update)
}



//-------------------------
//---- Shortcut traits ----
//-------------------------

trait RestReadController[R] extends Resource[R]
                               with ResourceRead

/**
 * Read and write controller: implements GET, POST and PATCH for partial updates
 */
trait RestRwController[R] extends Resource[R]
                             with ResourceCreate
                             with ResourceRead
                             with ResourceUpdate

/**
 * Same as RestRWController plus DELETE method
 */
trait RestRwdController[R] extends RestRwController[R]
                              with ResourceDelete

/**
 * Classic rest controller: handle GET, POST, PUT and DELETE http methods
 */
trait RestCrudController[R] extends Resource[R]
                               with ResourceCreate
                               with ResourceRead
                               with ResourceDelete
                               with ResourceWrite
