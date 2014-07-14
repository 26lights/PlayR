package twentysix.playr

import scala.language.reflectiveCalls
import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.annotation.Annotation
import scala.annotation.ClassfileAnnotation
import scala.annotation.StaticAnnotation
import twentysix.playr.core.BaseResource
import twentysix.playr.core.ResourceAction
import twentysix.playr.core.ControllerFactory


sealed abstract class Routing[C<:BaseResource] {
  def routing( controller: C,
               requestHeader: RequestHeader,
               sid: String,
               prefix: String,
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
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      def next(id: C#IdentifierType) =
        Router.Include {
          val subRouter = router.withParent(controller, id, RouteFilterContext(name, Some(sid), Some(id), parentContext))
          subRouter.setPrefix(prefix)
          subRouter
        }.unapply(requestHeader)
      wrapper.routeFilterWrapper.filterTraverse(controller, requestHeader, name, sid, parentContext, next)
    }
    def routeInfo = router.routeResource

    val custom = false
  }

  class ResourceRouting(val router: RestResourceRouter[_]) extends Routing[C]{
    def routing( controller: C,
                 requestHeader: RequestHeader,
                 sid: String,
                 prefix: String,
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      def next(id: C#IdentifierType) = Router.Include {
        val subRouter = router.withParentContext(RouteFilterContext(name, Some(sid), Some(id), parentContext))
        subRouter.setPrefix(prefix)
        subRouter
      }.unapply(requestHeader)
      wrapper.routeFilterWrapper.filterTraverse(controller, requestHeader, name, sid, parentContext, next)
    }
    def routeInfo = router.routeResource
    val custom = false
  }

  class ActionRouting(val method: HttpMethod, val action: ResourceAction[C], route: String) extends Routing[C] {
    def routing( controller: C,
                 requestHeader: RequestHeader,
                 sid: String,
                 prefix: String,
                 parentContext: Option[RouteFilterContext[_]]): Option[Handler] = {
      if (method.name==requestHeader.method)
        wrapper.routeFilterWrapper.filterCustom(controller, requestHeader, route, sid, parentContext, id => action.handleAction(controller, id))
      else
        wrapper.routeFilterWrapper.filterCustom(controller, requestHeader, route, sid, parentContext, id => Some(Action { Results.MethodNotAllowed }))
    }
    def routeInfo = ActionRestRouteInfo(route, wrapper.controllerType, method)
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

  def add(route: String, method: HttpMethod, action: ResourceAction[C]): this.type =
    this.add(route-> new ActionRouting(method, action, route))
}


class RestResourceRouter[C<:BaseResource: ResourceWrapper]( val controller: C,
                                                            val path: Option[String] = None,
                                                            var routeMap: Map[String, Routing[C]]= Map[String, Routing[C]](),
                                                            val parentContext: Option[RouteFilterContext[_]] = None)
      extends AbstractRestResourceRouter[C] with RestRouter with SimpleRouter{
  val name = path.getOrElse(controller.name)

  private val methodNotAllowed = Action { Results.MethodNotAllowed }
  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val ROOT_OPTIONS = Map(
    ResourceCaps.Read   -> "GET",
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

  def handleRoute(requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String): Option[Handler] = {
    routeMap.get(subPath).flatMap { action =>
      action.routing(controller, requestHeader, sid, requestHeader.path.take(prefixLength + subPrefix.length), parentContext)
    }
  }

  def routeRequest(requestHeader: RequestHeader, path: String, method: String): Option[Handler] = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath)

      case "" | "/" => method match {
        case "GET"     => wrapper.readWrapper.list(controller, requestHeader, name, parentContext)
        case "POST"    => wrapper.createWrapper(controller, requestHeader, name, parentContext)
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }

      case IdExpression(sid) => method match {
        case "GET"     => wrapper.readWrapper(controller, sid, requestHeader, name, parentContext)
        case "PUT"     => wrapper.writeWrapper(controller, sid, requestHeader, name, parentContext)
        case "DELETE"  => wrapper.deleteWrapper(controller, sid, requestHeader, name, parentContext)
        case "PATCH"   => wrapper.updateWrapper(controller, sid, requestHeader, name, parentContext)
        case "OPTIONS" => controller.parseId(sid).map(res => idOptionsRoutingHandler())
        case _         => controller.parseId(sid).map(res => methodNotAllowed)
      }

      case _ => None
    }
  }

  def withParentContext(context: RouteFilterContext[_]): RestResourceRouter[C] = new RestResourceRouter(controller, path, routeMap, Some(context))
}


class SubRestResourceRouter[P<:BaseResource, C <: BaseResource : ResourceWrapper](val name: String, val factory: ControllerFactory[P, C]) extends AbstractRestResourceRouter[C] {
  var routeMap = Map[String, Routing[C]]()
  override val caps = super.caps ++ ResourceCaps.ValueSet(ResourceCaps.Child)

  def withParent(parent: P, id: P#IdentifierType, context: RouteFilterContext[P#IdentifierType]) = new RestResourceRouter[C](
    factory.construct(parent, id),
    Some(name),
    routeMap,
    Some(context)
  )
}
