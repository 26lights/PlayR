package twentysix.playr.wrappers

import twentysix.playr.core.BaseResource
import play.api.mvc.{RequestHeader, Handler}
import twentysix.playr.core.ResourceRouteFilter
import twentysix.playr.RouteFilterContext

trait ResourceRouteFilterWrapper[T<:BaseResource] {
  def filterTraverse(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler]

  /*
  def filterList(controller: T, requestHeader: RequestHeader, path: String,
      parentContext: Option[RouteFilterContext[_]], next: () => Option[Handler]): Option[Handler]
  def filterCreate(controller: T, requestHeader: RequestHeader, path: String,
      parentContext: Option[RouteFilterContext[_]], next: () => Option[Handler]): Option[Handler]

  def filterRead(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler]
  def filterWrite(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler]
  def filterUpdate(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler]
  def filterCustom(controller: T, requestHeader: RequestHeader, path: String, sid: String,
      parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler]
  */
}
trait DefaultResourceRouteFilterWrapper {
  implicit def defaultImpl[T<:BaseResource] = new ResourceRouteFilterWrapper[T] {
    def filterTraverse(controller: T, requestHeader: RequestHeader, path: String, sid: String,
        parentContext: Option[RouteFilterContext[_]], next: T#IdentifierType => () => Option[Handler]): Option[Handler] =
      controller.parseId(sid).flatMap(next(_)())
  }
}
object ResourceRouteFilterWrapper extends DefaultResourceRouteFilterWrapper {
  implicit def resourceRouteFilterImpl[T<:BaseResource with ResourceRouteFilter] = new ResourceRouteFilterWrapper[T]{
    def filterTraverse(controller: T,
                       requestHeader: RequestHeader,
                       path: String,
                       sid: String,
                       parentContext: Option[RouteFilterContext[_]],
                       next: T#IdentifierType => () => Option[Handler]) = {
      val id = controller.parseId(sid)
      val nextFct = id.map(next(_)).getOrElse(()=>None)

      controller.routeFilter.filterTraverse(
        requestHeader, RouteFilterContext(path, Some(sid), id, parentContext), nextFct
      )
    }
  }
}

