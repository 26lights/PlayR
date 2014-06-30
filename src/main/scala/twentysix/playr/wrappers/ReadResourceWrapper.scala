package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceRead
import twentysix.playr.core.ResourceRouteFilter
import play.api.mvc.RequestHeader
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler

trait ReadResourceWrapper[T<:BaseResource] extends ResourceWrapperBase{
  def apply(obj: T, sid: String, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]): Option[Handler]
  def list(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]): Option[Handler]
}


trait DefaultReadResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseResource] = new ReadResourceWrapper[T] with DefaultApply[T]{
    def list(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = Some(methodNotAllowed)
  }
}

trait DefaultFilteredReadResourceWrapper extends DefaultReadResourceWrapper{
  implicit def filteredResourceImpl[T<:BaseResource with ResourceRouteFilter] = new ReadResourceWrapper[T] with DefaultCaps{
    def apply(obj: T, sid: String, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
      obj.routeFilter.filterRead( requestHeader,
                                  RouteFilterContext(path, Some(sid), obj.parseId(sid), parentContext),
                                  () => Some(methodNotAllowed))

    def list(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = {
      obj.routeFilter.filterList( requestHeader,
                                  RouteFilterContext(path, None, None, parentContext),
                                  () => Some(methodNotAllowed))
    }
  }
}


object ReadResourceWrapper extends DefaultFilteredReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResource with ResourceRead] = new ReadResourceWrapper[T]{
    def apply(obj: T, sid: String, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
      obj.parseId(sid).flatMap(obj.readResource(_))

    def list(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = obj.listResource

    val caps = ResourceCaps.ValueSet(ResourceCaps.Read)
  }

  implicit def readFilteredResourceImpl[T<:BaseResource with ResourceRead with ResourceRouteFilter] = new ReadResourceWrapper[T]{
    def apply(obj: T, sid: String, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) = {
      val id = obj.parseId(sid)
      obj.routeFilter.filterRead( requestHeader,
                                  RouteFilterContext(path, Some(sid), id, parentContext),
                                  nextFct(id, obj.readResource))
    }

    def list(obj: T, requestHeader: RequestHeader, path: String, parentContext: Option[RouteFilterContext[_]]) =
      obj.routeFilter.filterList(requestHeader, RouteFilterContext(path, None, None, parentContext), () => obj.listResource)

    val caps = ResourceCaps.ValueSet(ResourceCaps.Read)
  }
}

