package twentysix.playr

import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import twentysix.playr.RestRouteActionType._
import play.api.mvc.Action
import play.api.mvc.EssentialAction
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class TestFilter() extends SimpleRestRouteFilter[Boolean] {
  def filter(
      actionType: RestRouteActionType
  )(requestHeader: RequestHeader, context: RouteFilterContext[Boolean], next: () => Option[EssentialAction]) = {
    val nextAction = next()
    nextAction.map(TestFilter.testAction(actionType, context.contextPath))
  }

  def filterTraverse(
      requestHeader: RequestHeader,
      context: RouteFilterContext[Boolean],
      next: () => Option[Handler]
  ) = {
    next()
  }

}

object TestFilter {
  val TestHeader = "X-TEST-FILTER"
  val TestPathHeader = "X-TEST-PATH"

  def testAction(actionType: RestRouteActionType, path: String)(action: EssentialAction) = {
    EssentialAction { rh =>
      action(rh).map { result =>
        result.withHeaders(
          TestFilter.TestHeader -> actionType.toString(),
          TestFilter.TestPathHeader -> path
        )
      }
    }
  }
}
