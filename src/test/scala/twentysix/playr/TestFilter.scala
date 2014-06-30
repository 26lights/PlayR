package twentysix.playr

import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.RestRouteActionType._
import play.api.mvc.Action
import play.api.mvc.EssentialAction
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class TestFilter() extends SimpleRestRouteFilter[Boolean] {
  def filter( actionType: RestRouteActionType )
            ( requestHeader: RequestHeader,
              context: RouteFilterContext[Boolean],
              next: () => Option[EssentialAction] ) = {
    val nextAction = next()
    nextAction.map { action =>
      EssentialAction { rh =>
        action(rh).map { result =>
          result.withHeaders(TestFilter.TestHeader -> actionType.toString())
        }
      }
    }
  }

  def filterTraverse( requestHeader: RequestHeader,
                      context: RouteFilterContext[Boolean],
                      next: () => Option[Handler] ) = {
    next()
  }

}

object TestFilter {
  val TestHeader = "X-TEST-FILTER"
}