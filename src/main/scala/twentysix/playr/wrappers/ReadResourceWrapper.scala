package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceRead

trait ReadResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
  def list(obj: T): Option[EssentialAction]
}
trait DefaultReadResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseResource] = new ReadResourceWrapper[T] with DefaultApply[T]{
    def list(obj: T) = Some(methodNotAllowed)
  }
}
object ReadResourceWrapper extends DefaultReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResource with ResourceRead] = new ReadResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.parseId(sid).flatMap(obj.readResource(_))
    def list(obj: T) = obj.listResource
    val caps = ResourceCaps.ValueSet(ResourceCaps.Read)
  }
}

