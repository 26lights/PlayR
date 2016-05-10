package twentysix.playr

import play.core.routing.Include

import scala.language.reflectiveCalls
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.annotation.Annotation
import scala.annotation.ClassfileAnnotation
import scala.annotation.StaticAnnotation
import twentysix.playr.RestRouteActionType._
import twentysix.playr.core.BaseResource
import twentysix.playr.core.ResourceAction
import twentysix.playr.core.ControllerFactory


sealed abstract class Routing[C<:BaseResource] {
  def routing( controller: C,
               requestHeader: RequestHeader,
               sid: String,
               prefix: String,
               filter: Option[RestRouterFilter],
               parentContext: Option[RouteFilterContext[_]]): Option[Handler]
  def routeInfo: RestRouteInfo

  val custom: Boolean
}


abstract class AbstractRestResourceRouter[C<:BaseResource: ResourceWrapper] {
  val wrapper = implicitly[ResourceWrapper[C]]
  def caps = wrapper.readWrapper.caps ++
             wrapper.writeWrapper.caps ++
             wrapper.updateWrapper.caps ++
             wrapper.deleteWrapper.caps ++
             wrapper.listWrapper.caps ++
             wrapper.createWrapper.caps ++ {
               if(routeMap.isEmpty) ResourceCaps.ValueSet.empty
               else ResourceCaps.ValueSet(ResourceCaps.Parent)
             }

  val name: String
  var routeMap: Map[String, Routing[C]]

  class SubResourceRouting(val router: SubRestResourceRouter[C, _]) extends Routing[C]{
    def routing( controller: C,
                 requestHeader: RequestHeader,
                 sid: String,
                 prefix: String,
                 filter: Option[RestRouterFilter],
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      def next(id: C#IdentifierType) =
        Include {
          val subRouter = router.withParent(controller, id, RouteFilterContext(name, Some(sid), Some(id), parentContext), filter)
          subRouter.withPrefix(prefix)
        }.unapply(requestHeader)
      ApplyRouterFilter(filter, Traverse, RouteFilterContext.pathWithParent(parentContext, name), requestHeader) { () =>
        wrapper.routeFilterWrapper.filterTraverse(controller, requestHeader, name, sid, parentContext, next)
      }
    }
    def routeInfo = router.routeResource

    val custom = false
  }

  class ResourceRouting(val router: RestResourceRouter[_]) extends Routing[C]{
    def routing( controller: C,
                 requestHeader: RequestHeader,
                 sid: String,
                 prefix: String,
                 filter: Option[RestRouterFilter],
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      def next(id: C#IdentifierType) = Include {
        val subRouter = router.withParentContext(RouteFilterContext(name, Some(sid), Some(id), parentContext), filter)
        subRouter.withPrefix(prefix)
      }.unapply(requestHeader)
      ApplyRouterFilter(filter, Traverse, RouteFilterContext.pathWithParent(parentContext, name), requestHeader) { () =>
        wrapper.routeFilterWrapper.filterTraverse(controller, requestHeader, name, sid, parentContext, next)
      }
    }
    def routeInfo = router.routeResource
    val custom = false
  }

  class ActionRouting(val actions: PartialFunction[HttpMethod, ResourceAction[C]], route: String, attributes: Map[HttpMethod, Map[Any, Any]]) extends Routing[C] {
    lazy val supportedHttpMethods = Set(GET, POST, DELETE, PUT, PATCH).filter( actions.isDefinedAt(_) )

    def routing( controller: C,
                 requestHeader: RequestHeader,
                 sid: String,
                 prefix: String,
                 filter: Option[RestRouterFilter],
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      val next: (C#IdentifierType) => Option[EssentialAction] =
        HttpMethod.All.get(requestHeader.method).map {
          actions.andThen { action =>
            id: C#IdentifierType => action.handleAction(controller, id)
          } orElse {
            case OPTIONS =>
              id: C#IdentifierType => Some(Action {
                Results.Ok.withHeaders(ALLOW -> supportedHttpMethods.mkString(", "))
              })
            case _ => id: C#IdentifierType => Some(Action { Results.MethodNotAllowed })
          }
        }.getOrElse {
          id => Some(Action { Results.MethodNotAllowed })
        }
      ApplyRouterFilter(filter, Custom, RouteFilterContext.pathWithParent(parentContext, s"$name/$route" ), requestHeader) { () =>
        wrapper.routeFilterWrapper.filterCustom(controller, requestHeader, name, route, sid, parentContext, next)
      }
    }
    def routeInfo = ActionRestRouteInfo(route, wrapper.controllerType, supportedHttpMethods, attributes)
    val custom = true
  }

  def routeResource = ApiRestRouteInfo(name, wrapper.controllerType, caps, subRouteResources)
  def subRouteResources = routeMap.map { t => t._2.routeInfo }.toSeq

  def add(t: (String, Routing[C])): this.type = {
    routeMap = routeMap + t
    this
  }

  def add(router: SubRestResourceRouter[C, _]): this.type = add(router.name -> new SubResourceRouting(router))
  def add(router: RestResourceRouter[_]): this.type = this.add(router.name -> new ResourceRouting(router))

  def add[S<:BaseResource : ResourceWrapper](route: String, factory: ControllerFactory[C, S]): this.type =
    this.add(new SubRestResourceRouter(route, factory))

  def add(route: String, attributes: Map[HttpMethod, Map[Any, Any]])(actions: PartialFunction[HttpMethod, ResourceAction[C]]): this.type =
    this.add(route-> new ActionRouting(actions, route, attributes))

  def add(route: String, method: HttpMethod, action: ResourceAction[C], attributes: Map[Any, Any]): this.type = {
    this.add(route, Map(method -> attributes)) {
      case `method` => action
    }
  }

  def add(route: String)(actions: PartialFunction[HttpMethod, ResourceAction[C]]): this.type =
    this.add(route-> new ActionRouting(actions, route, Map()))

  def add(route: String, method: HttpMethod, action: ResourceAction[C]): this.type =
    this.add(route, method, action, Map())

  def addSubRouter[S<:BaseResource : ResourceWrapper](route: String, factory: ControllerFactory[C, S])(block: SubRestResourceRouter[C, S] => SubRestResourceRouter[C, S]): this.type =
    this.add(block(new SubRestResourceRouter(route, factory)))
}


class RestResourceRouter[C<:BaseResource: ResourceWrapper]( val controller: C,
                                                            val path: Option[String] = None,
                                                            var routeMap: Map[String, Routing[C]]= Map[String, Routing[C]](),
                                                            val filter: Option[RestRouterFilter] = None,
                                                            val parentContext: Option[RouteFilterContext[_]] = None)
      extends AbstractRestResourceRouter[C] with RestRouter with SimpleRouter{
  val name = path.getOrElse(controller.name)

  private lazy val contextPath = RouteFilterContext.pathWithParent(parentContext, name)

  private val methodNotAllowed = Action { Results.MethodNotAllowed }
  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val ROOT_OPTIONS = Map(
    ResourceCaps.List   -> "GET",
    ResourceCaps.Create -> "POST"
  )
  private val ID_OPTIONS = Map(
    ResourceCaps.Read   -> "GET",
    ResourceCaps.Delete -> "DELETE",
    ResourceCaps.Write  -> "PUT",
    ResourceCaps.Update -> "PATCH"
  )

  def optionsRoutingHandler(map: Map[ResourceCaps.Value, String]) = Action {
    val options = map.filterKeys( caps.contains ).values mkString ","
    Results.Ok.withHeaders(ALLOW -> options)
  }

  def rootOptionsRoutingHandler = optionsRoutingHandler(ROOT_OPTIONS)
  def idOptionsRoutingHandler = optionsRoutingHandler(ID_OPTIONS)

  def handleRoute(requestHeader: RequestHeader, subPrefix: String, sid: String, subPath: String): Option[Handler] = {
    routeMap.get(subPath).flatMap { action =>
      action.routing(controller, requestHeader, sid, requestHeader.path.take(subPrefix.length), filter, parentContext)
    }
  }

  def routeRequest(requestHeader: RequestHeader, path: String, method: String): Option[Handler] = {
    def filterRoute(action: RestRouteActionType, block: () => Option[Handler]) = {
      ApplyRouterFilter(filter, action, contextPath, requestHeader)(block)
    }

    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        handleRoute(requestHeader, subPrefix, id, subPath)

      case "" | "/" => method match {
        case "GET"     => filterRoute(List,   () => wrapper.listWrapper(controller, requestHeader, name, parentContext))
        case "POST"    => filterRoute(Create, () => wrapper.createWrapper(controller, requestHeader, name, parentContext))
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }

      case IdExpression(sid) => method match {
        case "GET"     => filterRoute(Read,   () => wrapper.readWrapper(controller, sid, requestHeader, name, parentContext))
        case "PUT"     => filterRoute(Write,  () => wrapper.writeWrapper(controller, sid, requestHeader, name, parentContext))
        case "DELETE"  => filterRoute(Delete, () => wrapper.deleteWrapper(controller, sid, requestHeader, name, parentContext))
        case "PATCH"   => filterRoute(Update, () => wrapper.updateWrapper(controller, sid, requestHeader, name, parentContext))
        case "OPTIONS" => controller.parseId(sid).map(res => idOptionsRoutingHandler())
        case _         => controller.parseId(sid).map(res => methodNotAllowed)
      }

      case _ => None
    }
  }

  def withFilter(f: RestRouterFilter) = new RestResourceRouter(controller, path, routeMap, Some(f))

  def withParentContext(context: RouteFilterContext[_], parentFilter: Option[RestRouterFilter]): RestResourceRouter[C] =
    new RestResourceRouter(controller, path, routeMap, filter orElse parentFilter, Some(context))
}


class SubRestResourceRouter[P<:BaseResource, C <: BaseResource : ResourceWrapper](val name: String, val factory: ControllerFactory[P, C]) extends AbstractRestResourceRouter[C] {
  var routeMap = Map[String, Routing[C]]()
  var filter: Option[RestRouterFilter] = None
  override val caps = super.caps ++ ResourceCaps.ValueSet(ResourceCaps.Child)

  def withParent(parent: P, id: P#IdentifierType, context: RouteFilterContext[P#IdentifierType], parentFilter: Option[RestRouterFilter]) = new RestResourceRouter[C](
    factory.construct(parent, id),
    Some(name),
    routeMap,
    filter orElse parentFilter,
    Some(context)
  )
}
