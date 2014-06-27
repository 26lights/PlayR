package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceDelete

trait DeleteResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultDeleteResourceWrapper{
  implicit def defaultImpl[T<:BaseResource] = new DeleteResourceWrapper[T] with DefaultApply[T]
}
object DeleteResourceWrapper extends DefaultDeleteResourceWrapper{
  implicit def deleteResourceImpl[T<:BaseResource with ResourceDelete] = new DeleteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.parseId(sid).flatMap(obj.deleteResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Delete)
  }
}

