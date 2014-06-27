package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.Action
import play.api.mvc.Results

trait ResourceWrapperBase {
  val methodNotAllowed = Action { Results.MethodNotAllowed }
  val caps: ResourceCaps.ValueSet
}

trait DefaultCaps{
  val caps = ResourceCaps.ValueSet.empty
}

trait DefaultApply[T<:BaseResource] extends DefaultCaps {
  this: ResourceWrapperBase  =>
  def apply(obj: T, sid: String) = obj.parseId(sid).map( _ => methodNotAllowed)
//  def apply(obj: T): (T#IdentifierType => Handler) = (id: T#IdentifierType) => methodNotAllowed
}

