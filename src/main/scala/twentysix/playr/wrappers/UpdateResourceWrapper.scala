package twentysix.playr.wrappers

import twentysix.playr.ResourceCaps
import twentysix.playr.core.BaseResource
import play.api.mvc.EssentialAction
import twentysix.playr.core.ResourceUpdate
import twentysix.playr.RouteFilterContext
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.core.ResourceRouteFilter

trait UpdateResourceWrapper[T <: BaseResource] extends ResourceWrapperBase {
  def apply(
      obj: T,
      sid: String,
      requestHeader: RequestHeader,
      path: String,
      parentContext: Option[RouteFilterContext[_]]
  ): Option[Handler]
}

trait DefaultUpdateResourceWrapper {
  implicit def defaultImpl[T <: BaseResource] = new UpdateResourceWrapper[T] with DefaultApply[T]
}

trait DefaultFilteredUpdateResourceWrapper extends DefaultUpdateResourceWrapper {
  implicit def defaultFilteredImpl[T <: BaseResource with ResourceRouteFilter] =
    new UpdateResourceWrapper[T] with DefaultCaps {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) =
        obj.routeFilter.filterUpdate(
          requestHeader,
          RouteFilterContext(path, Some(sid), obj.parseId(sid), parentContext),
          () => Some(methodNotAllowed(obj.Action))
        )
    }
}

object UpdateResourceWrapper extends DefaultFilteredUpdateResourceWrapper {
  implicit def updateResourceImpl[T <: BaseResource with ResourceUpdate] = new UpdateResourceWrapper[T] {
    def apply(
        obj: T,
        sid: String,
        requestHeader: RequestHeader,
        path: String,
        parentContext: Option[RouteFilterContext[_]]
    ) =
      obj.parseId(sid).flatMap(obj.updateResource(_))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Update)
  }

  implicit def updateFilteredResourceImpl[T <: BaseResource with ResourceUpdate with ResourceRouteFilter] =
    new UpdateResourceWrapper[T] {
      def apply(
          obj: T,
          sid: String,
          requestHeader: RequestHeader,
          path: String,
          parentContext: Option[RouteFilterContext[_]]
      ) = {
        val id = obj.parseId(sid)
        obj.routeFilter.filterUpdate(
          requestHeader,
          RouteFilterContext(path, Some(sid), id, parentContext),
          nextFct(id, obj.updateResource)
        )
      }
      val caps = ResourceCaps.ValueSet(ResourceCaps.Update)
    }
}
