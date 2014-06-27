package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceWrite

trait WriteResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultWriteResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new WriteResourceWrapper[T] with DefaultApply[T]
}
object WriteResourceWrapper extends DefaultWriteResourceWrapper{
  implicit def writeResourceImpl[T<:BaseResource with ResourceWrite] = new WriteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.parseId(sid).flatMap(obj.writeResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Write)
  }
}

