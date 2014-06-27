package twentysix.playr

import play.api.mvc.SimpleResult
import play.api.mvc.RequestHeader
import RestRouteActionType._
import play.api.mvc.Handler

case class RouteFilterContext[T](path: String, sid: Option[String], id: Option[T], parent: Option[RouteFilterContext[_]])

trait RestRouteFilter[T] {
  type _FilterType = ( RequestHeader, RouteFilterContext[T] , () => Option[Handler]) => Option[Handler]

  def filterTraverse: _FilterType
  def filterRead: _FilterType
  def filterWrite: _FilterType
  def filterUpdate: _FilterType
  def filterCustom: _FilterType
  def filterList: _FilterType
  def filterCreate: _FilterType
}


trait SimpleRestRouteFilter[T] extends RestRouteFilter[T] {
  def filter( actionType: RestRouteActionType )
            ( requestHeader: RequestHeader,
              context: RouteFilterContext[T],
              next: () => Option[Handler] ) : Option[Handler]

  def filterTraverse = filter(Traverse)
  def filterRead = filter(Read)
  def filterWrite = filter(Write)
  def filterUpdate = filter(Update)
  def filterCustom = filter(Custom)
  def filterList = filter(List)
  def filterCreate = filter(Create)
}


case class NoopFilter() extends SimpleRestRouteFilter[Any] {
  def filter( actionType: RestRouteActionType )( requestHeader: RequestHeader,
                                                 context: RouteFilterContext[Any],
                                                 next: ()=> Option[Handler] ) =
    next()
}
