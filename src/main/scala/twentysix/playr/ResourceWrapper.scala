package twentysix.playr

import scala.reflect.runtime.universe._
import play.api.mvc._

sealed trait ResourceWrapperBase {
  val methodNotAllowed = Action { Results.MethodNotAllowed }
  val caps: ResourceCaps.ValueSet
}

sealed trait DefaultCaps{
  val caps = ResourceCaps.ValueSet.empty
}

sealed trait DefaultApply[T<:BaseResource] extends DefaultCaps {
  this: ResourceWrapperBase  =>
  def apply(obj: T, sid: String) = obj.requestWrapper {
    obj.fromId(sid).map(_ => methodNotAllowed)
  }
}

trait ReadResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
  def list(obj: T): EssentialAction
}
trait DefaultReadResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseResource] = new ReadResourceWrapper[T] with DefaultApply[T]{
    def list(obj: T) = methodNotAllowed
  }
}
object ReadResourceWrapper extends DefaultReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResourceRead] = new ReadResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.readRequestWrapper {
      obj.fromId(sid).map(obj.read)
    }
    def list(obj: T): EssentialAction = obj.list
    val caps = ResourceCaps.ValueSet(ResourceCaps.Read)
  }
}

trait WriteResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultWriteResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new WriteResourceWrapper[T] with DefaultApply[T]
}
object WriteResourceWrapper extends DefaultWriteResourceWrapper{
  implicit def writeResourceImpl[T<:BaseResourceWrite] = new WriteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.writeRequestWrapper {
      obj.fromId(sid).map(obj.write)
    }
    val caps = ResourceCaps.ValueSet(ResourceCaps.Write)
  }
}


trait UpdateResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultUpdateResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new UpdateResourceWrapper[T] with DefaultApply[T]
}
object UpdateResourceWrapper extends DefaultUpdateResourceWrapper{
  implicit def updateResourceImpl[T<:BaseResourceUpdate] = new UpdateResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.updateRequestWrapper {
      obj.fromId(sid).map(obj.update)
    }
    val caps = ResourceCaps.ValueSet(ResourceCaps.Update)
  }
}

trait DeleteResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultDeleteResourceWrapper{
  implicit def defaultImpl[T<:BaseResource] = new DeleteResourceWrapper[T] with DefaultApply[T]
}
object DeleteResourceWrapper extends DefaultDeleteResourceWrapper{
  implicit def deleteResourceImpl[T<:BaseResourceDelete] = new DeleteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.deleteRequestWrapper {
      obj.fromId(sid).map(obj.delete)
    }
    val caps = ResourceCaps.ValueSet(ResourceCaps.Delete)
  }
}

trait CreateResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T): EssentialAction
}
trait DefaultCreateResourceWrapper {
  implicit def defaultImpl[T] = new CreateResourceWrapper[T] with DefaultCaps{
    def apply(obj: T) = methodNotAllowed
  }
}
object CreateResourceWrapper extends DefaultCreateResourceWrapper{
  implicit def createResourceImpl[T<:BaseResourceCreate] = new CreateResourceWrapper[T]{
    def apply(obj: T) = obj.create
    val caps = ResourceCaps.ValueSet(ResourceCaps.Create)
  }
}

trait ResourceWrapper[T]{
  def readWrapper: ReadResourceWrapper[T]
  def writeWrapper: WriteResourceWrapper[T]
  def updateWrapper: UpdateResourceWrapper[T]
  def deleteWrapper: DeleteResourceWrapper[T]
  def createWrapper: CreateResourceWrapper[T]
  def controllerType: Type
}
object ResourceWrapper {
  implicit def resourceWrapperImpl[C<:BaseResource
                                     :TypeTag
                                     :ReadResourceWrapper
                                     :WriteResourceWrapper
                                     :UpdateResourceWrapper
                                     :DeleteResourceWrapper
                                     :CreateResourceWrapper] =
    new ResourceWrapper[C] {
      val readWrapper = implicitly[ReadResourceWrapper[C]]
      val writeWrapper = implicitly[WriteResourceWrapper[C]]
      val updateWrapper = implicitly[UpdateResourceWrapper[C]]
      val deleteWrapper = implicitly[DeleteResourceWrapper[C]]
      val createWrapper = implicitly[CreateResourceWrapper[C]]
      val controllerType = typeOf[C]
  }
}
