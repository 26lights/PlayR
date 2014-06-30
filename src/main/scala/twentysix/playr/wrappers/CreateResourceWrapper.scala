package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceCreate
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.core.ResourceRouteFilter

trait CreateResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]): Option[Handler]
}

trait DefaultCreateResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new CreateResourceWrapper[T] with DefaultCaps{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = Some(methodNotAllowed)
  }
}

trait DefaultFilteredCreateResourceWrapper extends DefaultCreateResourceWrapper{
  implicit def defaultFilteredImpl[T<:BaseResource with ResourceRouteFilter] = new CreateResourceWrapper[T] with DefaultCaps{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = {
      obj.routeFilter.filterCreate( requestHeader,
                                    RouteFilterContext(path, None, None, parentContext),
                                    () => Some(methodNotAllowed))
    }
  }
}

object CreateResourceWrapper extends DefaultFilteredCreateResourceWrapper{
  implicit def createResourceImpl[T<:BaseResource with ResourceCreate] = new CreateResourceWrapper[T]{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = obj.createResource
    val caps = ResourceCaps.ValueSet(ResourceCaps.Create)
  }

  implicit def createResourceFilteredImpl[T<:BaseResource with ResourceCreate with ResourceRouteFilter] = new CreateResourceWrapper[T]{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
      obj.routeFilter.filterCreate(requestHeader, RouteFilterContext(path, None, None, parentContext), () => obj.createResource)
    val caps = ResourceCaps.ValueSet(ResourceCaps.Create)
  }
}

