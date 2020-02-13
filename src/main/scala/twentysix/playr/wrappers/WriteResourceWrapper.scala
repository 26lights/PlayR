package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceWrite
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.core.ResourceRouteFilter

trait WriteResourceWrapper[T <: BaseResource] extends ResourceWrapperBase {
  def apply(
      obj: T,
      sid: String,
      requestHeader: RequestHeader,
      path: String,
      parentContext: Option[RouteFilterContext[_]]
  ): Option[Handler]
}

trait DefaultWriteResourceWrapper {
  implicit def defaultImpl[T <: BaseResource] = new WriteResourceWrapper[T] with DefaultApply[T]
}

trait DefaultFilteredWriteResourceWrapper extends DefaultWriteResourceWrapper {
  implicit def defaultFilteredImpl[T <: BaseResource with ResourceRouteFilter] =
    new WriteResourceWrapper[T] with DefaultCaps {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) =
        obj.routeFilter.filterWrite(
          requestHeader,
          RouteFilterContext(path, Some(sid), obj.parseId(sid), parentContext),
          () => Some(methodNotAllowed(obj.Action))
        )
    }
}

object WriteResourceWrapper extends DefaultFilteredWriteResourceWrapper {
  implicit def writeResourceImpl[T <: BaseResource with ResourceWrite] = new WriteResourceWrapper[T] {
    def apply(
        obj: T,
        sid: String,
        requestHeader: RequestHeader,
        path: String,
        parentContext: Option[RouteFilterContext[_]]
    ) =
      obj.parseId(sid).flatMap(obj.writeResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Write)
  }

  implicit def writeFilteredResourceImpl[T <: BaseResource with ResourceWrite with ResourceRouteFilter] =
    new WriteResourceWrapper[T] {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) = {
        val id = obj.parseId(sid)
        obj.routeFilter.filterWrite(
          requestHeader,
          RouteFilterContext(path, Some(sid), id, parentContext),
          nextFct(id, obj.writeResource)
        )
      }
      val caps = ResourceCaps.ValueSet(ResourceCaps.Write)
    }
}
