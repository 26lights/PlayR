package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceUpdate

trait UpdateResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultUpdateResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new UpdateResourceWrapper[T] with DefaultApply[T]
}
object UpdateResourceWrapper extends DefaultUpdateResourceWrapper{
  implicit def updateResourceImpl[T<:BaseResource with ResourceUpdate] = new UpdateResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.parseId(sid).flatMap(obj.updateResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Update)
  }
}

