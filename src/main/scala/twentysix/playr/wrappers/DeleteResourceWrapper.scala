package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceDelete
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.core.ResourceRouteFilter

trait DeleteResourceWrapper[T <: BaseResource] extends ResourceWrapperBase {
  def apply(
      obj: T,
      sid: String,
      requestHeader: RequestHeader,
      path: String,
      parentContext: Option[RouteFilterContext[_]]
  ): Option[Handler]
}

trait DefaultDeleteResourceWrapper {
  implicit def defaultImpl[T <: BaseResource] = new DeleteResourceWrapper[T] with DefaultApply[T]
}

trait DefaultFilteredDeleteResourceWrapper extends DefaultDeleteResourceWrapper {
  implicit def defaultFilteredImpl[T <: BaseResource with ResourceRouteFilter] =
    new DeleteResourceWrapper[T] with DefaultCaps {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) =
        obj.routeFilter.filterDelete(
          requestHeader,
          RouteFilterContext(path, Some(sid), obj.parseId(sid), parentContext),
          () => Some(methodNotAllowed(obj.Action))
        )
    }
}

object DeleteResourceWrapper extends DefaultFilteredDeleteResourceWrapper {
  implicit def deleteResourceImpl[T <: BaseResource with ResourceDelete] = new DeleteResourceWrapper[T] {
    def apply(
        obj: T,
        sid: String,
        requestHeader: RequestHeader,
        path: String,
        parentContext: Option[RouteFilterContext[_]]
    ) =
      obj.parseId(sid).flatMap(obj.deleteResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Delete)
  }

  implicit def deleteFilteredResourceImpl[T <: BaseResource with ResourceDelete with ResourceRouteFilter] =
    new DeleteResourceWrapper[T] {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) = {
        val id = obj.parseId(sid)
        obj.routeFilter.filterDelete(
          requestHeader,
          RouteFilterContext(path, Some(sid), id, parentContext),
          nextFct(id, obj.deleteResource)
        )
      }
      val caps = ResourceCaps.ValueSet(ResourceCaps.Delete)
    }
}
