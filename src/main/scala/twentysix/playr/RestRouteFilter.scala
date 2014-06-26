package twentysix.playr

import play.api.mvc.SimpleResult
import play.api.mvc.RequestHeader
import RestRouteActionType._

case class RouteFilterContext[T](path: String, sid: Option[String], id: Option[T], parent: Option[RouteFilterContext[_]])

trait BaseRestRouteFilter {
  type ResourceType

  type _FilterType = ( RequestHeader, RouteFilterContext[ResourceType] , => Option[SimpleResult]) => Option[SimpleResult]

  def filterTraverse: _FilterType
  def filterRead: _FilterType
  def filterWrite: _FilterType
  def filterUpdate: _FilterType
  def filterCustom: _FilterType
  def filterList: _FilterType
  def filterCreate: _FilterType
}

trait RestRouteFilter[T] extends BaseRestRouteFilter{
  type ResourceType = T
}

trait SimpleRestRouteFilter[T] extends RestRouteFilter[T] {
  def filter( actionType: RestRouteActionType )
            ( requestHeader: RequestHeader,
              context: RouteFilterContext[T],
              next: => Option[SimpleResult] ) : Option[SimpleResult]

  def filterTraverse = filter(Traverse)
  def filterRead = filter(Read)
  def filterWrite = filter(Write)
  def filterUpdate = filter(Update)
  def filterCustom = filter(Custom)
  def filterList = filter(List)
  def filterCreate = filter(Create)
}


object NoopFilter extends SimpleRestRouteFilter[Any] {
  def filter( actionType: RestRouteActionType )( requestHeader: RequestHeader, context: RouteFilterContext[Any], next: => Option[SimpleResult] ) = next
}