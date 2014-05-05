package twentysix.playr.simple

import play.api.mvc.EssentialAction
import twentysix.playr.core
import twentysix.playr.core.ResourceAction
import reflect.runtime.universe.{Type,TypeTag,typeOf}
import scala.language.implicitConversions
import twentysix.playr.ResourceWrapper
import twentysix.playr.core.ControllerFactory

trait Resource[R] extends core.ResourceTrait[R]
                     with core.ResourceShortcuts{
  def parseId(sid: String) = fromId(sid)

  def handleAction(id: R, f: Function1[R, EssentialAction]) = Some(f(id))
  def fromId(sid: String): Option[R]
}

object Resource {
  implicit def simpleResourceAction[R, C<:Resource[R]](f: R=> EssentialAction)(implicit tt: TypeTag[R=> EssentialAction]) =
    new ResourceAction[C]{
      def handleAction(controller: C, id: R): Option[EssentialAction] = controller.handleAction(id, f)
      def getType: Type = tt.tpe
    }

  implicit def simpleSubResourceAction[R, C<:Resource[R]](f: C => R => EssentialAction)(implicit tt: TypeTag[R=> EssentialAction]) =
    new ResourceAction[C] {
      def handleAction(controller: C, id: R): Option[EssentialAction] = controller.handleAction(id, f(controller))
      def getType: Type = tt.tpe
    }

  implicit def simpleControllerFactory[R, P<:Resource[R], C<:core.BaseResource: ResourceWrapper](f: R => C ) =
    new ControllerFactory[P, C]{
      def construct(parent: P, resource: R) = f(resource)
    }
}

trait ResourceRead extends core.ResourceRead {
  this: core.BaseResource =>
  def readResource(id: IdentifierType) = Some(read(id))
  def listResource = Some(list)

  def read(id: IdentifierType): EssentialAction
  def list: EssentialAction
}

trait ResourceWrite extends core.ResourceWrite{
  this: core.BaseResource =>
  def writeResource(id: IdentifierType) = Some(write(id))

  def write(id: IdentifierType): EssentialAction
}

trait ResourceDelete extends core.ResourceDelete {
  this: core.BaseResource =>
  def deleteResource(id: IdentifierType) = Some(delete(id))

  def delete(id: IdentifierType): EssentialAction
}

trait ResourceUpdate extends core.ResourceUpdate {
  this: core.BaseResource =>
  def updateResource(id: IdentifierType) = Some(update(id))

  def update(id: IdentifierType): EssentialAction
}

trait ResourceCreate extends core.ResourceCreate {
  this: core.BaseResource =>
  def createResource = Some(create)

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
