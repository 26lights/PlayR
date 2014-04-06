package twentysix.playr.simple

import play.api.mvc.EssentialAction
import twentysix.playr.core

trait Resource[R] extends core.ResourceTrait[R]


trait ResourceRead extends core.ResourceRead {
  this: core.BaseResource => 
  def readResource(id: IdentifierType) = read(id)
  def listResource = list

  def read(id: IdentifierType): EssentialAction
  def list: EssentialAction
}

trait ResourceWrite extends core.ResourceWrite{
  this: core.BaseResource => 
  def writeResource(id: IdentifierType) = write(id) 

  def write(id: IdentifierType): EssentialAction
}

trait ResourceDelete extends core.ResourceDelete {
  this: core.BaseResource => 
  def deleteResource(id: IdentifierType) = delete(id) 

  def delete(id: IdentifierType): EssentialAction
}

trait ResourceUpdate extends core.ResourceUpdate {
  this: core.BaseResource => 
  def updateResource(id: IdentifierType) = update(id) 

  def update(id: IdentifierType): EssentialAction
}

trait ResourceCreate extends core.ResourceCreate {
  this: core.BaseResource => 
  def createResource = create 

  def create: EssentialAction
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
