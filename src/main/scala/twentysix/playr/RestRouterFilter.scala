package twentysix.playr

import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.RestRouteActionType.RestRouteActionType

case class RouterFilterContext(action: RestRouteActionType, path: String)

trait RestRouterFilter {
  type FilterFunction = ( RequestHeader, () => Option[Handler] ) => Option[Handler]

  def filter: PartialFunction[RouterFilterContext, FilterFunction]
}

object ApplyRouterFilter {
  def reject( rh: RequestHeader, next: () => Option[Handler] ): Option[Handler] = None

  def apply(filter: Option[RestRouterFilter], action: RestRouteActionType, path: String, requestHeader: RequestHeader)(block: () => Option[Handler]): Option[Handler] = {
    filter.map { f =>
      f.filter.applyOrElse(RouterFilterContext(action, path), (_: RouterFilterContext) => reject _)(requestHeader, block)
    }.getOrElse(block())
  }
}

object NoopRouterFilter extends RestRouterFilter {
  def filterFunction(requestHeader: RequestHeader, next: () => Option[Handler]) = next()

  def filter = {
    case _ => filterFunction _
  }
}
