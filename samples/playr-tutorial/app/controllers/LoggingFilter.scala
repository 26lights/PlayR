package controllers

import play.api._
import play.api.mvc._
import twentysix.playr.RestApiRouter
import twentysix.playr.RestResourceRouter
import twentysix.playr.GET
import twentysix.playr.ApiInfo
import twentysix.playr.RootApiRouter
import twentysix.playr.SubRestResourceRouter
import models.Company
import twentysix.playr.simple.ResourceRouteFilter
import twentysix.playr.RestRouteFilter
import twentysix.playr.SimpleRestRouteFilter
import twentysix.playr.RouteFilterContext
import twentysix.playr.RestRouteActionType._

trait LoggingFilter extends ResourceRouteFilter {
  val routeFilter: RestRouteFilter[IdentifierType] = new SimpleRestRouteFilter[IdentifierType] {
    def filter( actionType: RestRouteActionType )
              ( requestHeader: RequestHeader,
                context: RouteFilterContext[IdentifierType],
                next: () => Option[EssentialAction] ) : Option[EssentialAction] = {
      Logger.debug(s"[filter] path=${context.path} (${context.contextPath}) - action $actionType")
      next()
    }

    def filterTraverse( requestHeader: RequestHeader, context: RouteFilterContext[IdentifierType], next: () => Option[Handler] ) = {
      Logger.debug(s"[filter] traverse path=${context.path} (${context.contextPath})")
      next()
    }
  }
}
