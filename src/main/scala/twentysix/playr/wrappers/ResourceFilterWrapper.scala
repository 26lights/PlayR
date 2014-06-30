package twentysix.playr.wrappers

import play.api.mvc.{RequestHeader, Handler}
import twentysix.playr.core.BaseResource
import twentysix.playr.core.ResourceRouteFilter
import twentysix.playr.RouteFilterContext
import play.api.mvc.EssentialAction

trait ResourceRouteFilterWrapper[T<:BaseResource] extends ResourceWrapperBase {
  def filterTraverse(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[Handler]): Option[Handler]
  def filterCustom(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[EssentialAction]): Option[EssentialAction]
}


trait DefaultResourceRouteFilterWrapper {
  implicit def defaultImpl[T<:BaseResource] = new ResourceRouteFilterWrapper[T] with DefaultCaps{
    def filterTraverse(controller: T, requestHeader: RequestHeader, path: String, sid: String,
        parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[Handler]): Option[Handler] =
        controller.parseId(sid).flatMap(next(_))

    def filterCustom(controller: T, requestHeader: RequestHeader, path: String, sid: String,
        parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[EssentialAction]) =
      controller.parseId(sid).flatMap(next(_))
  }
}


object ResourceRouteFilterWrapper extends DefaultResourceRouteFilterWrapper {
  implicit def resourceRouteFilterImpl[T<:BaseResource with ResourceRouteFilter] = new ResourceRouteFilterWrapper[T] with DefaultCaps {
    def filterTraverse(controller: T, requestHeader: RequestHeader, path: String, sid: String,
        parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[Handler]) = {
      val id = controller.parseId(sid)
      controller.routeFilter.filterTraverse(
        requestHeader, RouteFilterContext(path, Some(sid), id, parentContext), nextFct(id, next)
      )
    }
    def filterCustom(controller: T, requestHeader: RequestHeader, path: String, sid: String,
        parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => Option[EssentialAction]) = {
      val id = controller.parseId(sid)
      controller.routeFilter.filterCustom(
        requestHeader, RouteFilterContext(path, Some(sid), id, parentContext), nextFct(id, next)
      )
    }
  }
}

