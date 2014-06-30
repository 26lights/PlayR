package twentysix.playr.wrappers

import play.api.mvc.{Action, RequestHeader, Results}
import twentysix.playr.RouteFilterContext
import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.Handler

trait ResourceWrapperBase {
  val methodNotAllowed = Action { Results.MethodNotAllowed }
  val caps: ResourceCaps.ValueSet

  def nextFct[T, R](idOpt: Option[T], fct: T => Option[R]): () => Option[R] =
    idOpt.map(id => () => fct(id)).getOrElse( () => None )
}

trait DefaultCaps{
  val caps = ResourceCaps.ValueSet.empty
}

trait DefaultApply[T<:BaseResource] extends DefaultCaps {
  this: ResourceWrapperBase  =>
  def apply(obj: T, sid: String) = obj.parseId(sid).map( _ => methodNotAllowed)
  def apply(obj: T, sid: String, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
    obj.parseId(sid).map( _ => methodNotAllowed)
}

