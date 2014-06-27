package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceCreate

trait CreateResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T): Option[EssentialAction]
}
trait DefaultCreateResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new CreateResourceWrapper[T] with DefaultCaps{
    def apply(obj: T) = Some(methodNotAllowed)
  }
}
object CreateResourceWrapper extends DefaultCreateResourceWrapper{
  implicit def createResourceImpl[T<:BaseResource with ResourceCreate] = new CreateResourceWrapper[T]{
    def apply(obj: T) = obj.createResource
    val caps = ResourceCaps.ValueSet(ResourceCaps.Create)
  }
}

