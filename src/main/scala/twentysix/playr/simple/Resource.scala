package twentysix.playr.simple

import scala.language.implicitConversions
import play.api.mvc.EssentialAction
import twentysix.playr.{ResourceWrapper, core}
import twentysix.playr.core.{ControllerFactory, ResourceAction}

trait BaseResource extends core.BaseResource {
  def parseId(sid: String) = fromId(sid)

  def handleAction(id: IdentifierType, f: Function1[IdentifierType, EssentialAction]) = Some(f(id))
  def fromId(sid: String): Option[IdentifierType]
}

trait Resource[R] extends BaseResource
                     with core.ResourceShortcuts{
  type IdentifierType = R
}

object Resource {
  implicit def simpleResourceAction[R, C<:Resource[R]](f: R=> EssentialAction) =
    new ResourceAction[C]{
      def handleAction(controller: C, id: R): Option[EssentialAction] = controller.handleAction(id, f)
    }

  implicit def simpleSubResourceAction[R, C<:Resource[R]](f: C => R => EssentialAction) =
    new ResourceAction[C] {
      def handleAction(controller: C, id: R): Option[EssentialAction] = controller.handleAction(id, f(controller))
    }

  implicit def simpleControllerFactory[R, P<:Resource[R], C<:core.BaseResource: ResourceWrapper](f: R => C ) =
    new ControllerFactory[P, C]{
      def construct(parent: P, resource: R) = f(resource)
    }
}

trait ResourceRead extends core.ResourceRead {
  this: BaseResource =>
  def readResource(id: IdentifierType) = Some(read(id))
  def listResource = Some(list)

  def read(id: IdentifierType): EssentialAction
  def list: EssentialAction
}

trait ResourceWrite extends core.ResourceWrite{
  this: BaseResource =>
  def writeResource(id: IdentifierType) = Some(write(id))

  def write(id: IdentifierType): EssentialAction
}

trait ResourceDelete extends core.ResourceDelete {
  this: BaseResource =>
  def deleteResource(id: IdentifierType) = Some(delete(id))

  def delete(id: IdentifierType): EssentialAction
}

trait ResourceUpdate extends core.ResourceUpdate {
  this: BaseResource =>
  def updateResource(id: IdentifierType) = Some(update(id))

  def update(id: IdentifierType): EssentialAction
}

trait ResourceCreate extends core.ResourceCreate {
  this: BaseResource =>
  def createResource = Some(create)

  def create: EssentialAction
}

trait ResourceRouteFilter extends BaseResource with core.ResourceRouteFilter

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
