package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceList
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.core.ResourceRouteFilter

trait ListResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]): Option[Handler]
}

trait DefaultListResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new ListResourceWrapper[T] with DefaultCaps{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = Some(methodNotAllowed)
  }
}

trait DefaultFilteredListResourceWrapper extends DefaultListResourceWrapper{
  implicit def defaultFilteredImpl[T<:BaseResource with ResourceRouteFilter] = new ListResourceWrapper[T] with DefaultCaps{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = {
      obj.routeFilter.filterList( requestHeader,
                                  RouteFilterContext(path, None, None, parentContext),
                                  () => Some(methodNotAllowed))
    }
  }
}

object ListResourceWrapper extends DefaultFilteredListResourceWrapper{
  implicit def listResourceImpl[T<:BaseResource with ResourceList] = new ListResourceWrapper[T]{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = obj.listResource
    val caps = ResourceCaps.ValueSet(ResourceCaps.List)
  }

  implicit def listResourceFilteredImpl[T<:BaseResource with ResourceList with ResourceRouteFilter] = new ListResourceWrapper[T]{
    def apply(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
      obj.routeFilter.filterList(requestHeader, RouteFilterContext(path, None, None, parentContext), () => obj.listResource)
    val caps = ResourceCaps.ValueSet(ResourceCaps.List)
  }
}

