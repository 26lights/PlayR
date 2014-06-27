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


sealed trait Routing[C<:BaseResource] {
  def routing( controller: C,
               id: C#IdentifierType,
               requestHeader: RequestHeader,
               prefix: String,
               context: RouteFilterContext[C#IdentifierType]): Option[Handler]
  def routeInfo(path: String): RestRouteInfo
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
                 id: C#IdentifierType,
                 requestHeader: RequestHeader,
                 prefix: String,
                 context: RouteFilterContext[C#IdentifierType]): Option[Handler] = {
      Router.Include {
        val subRouter = router.withParent(controller, id, context)
        subRouter.setPrefix(prefix)
        subRouter
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }

  class ResourceRouting(val router: RestResourceRouter[_]) extends Routing[C]{
    def routing( controller: C,
                 id: C#IdentifierType,
                 requestHeader: RequestHeader,
                 prefix: String,
                 context: RouteFilterContext[C#IdentifierType]): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }

  class ActionRouting(val method: HttpMethod, val action: ResourceAction[C], route: String) extends Routing[C] {
    def routing( controller: C,
                 id: C#IdentifierType,
                 requestHeader: RequestHeader,
                 prefix: String,
                 context: RouteFilterContext[C#IdentifierType]): Option[Handler] = {
        if (method.name==requestHeader.method)
          action.handleAction(controller, id)
        else
          Some(Action { Results.MethodNotAllowed })
    }
    def routeInfo(path: String) = ActionRestRouteInfo(path, route, wrapper.controllerType, ResourceCaps.ValueSet(ResourceCaps.Action), Seq(), method)
  }

  def routeResources(root: String) = Seq(routerRouteResource(root))
  def routerRouteResource(root: String) = ApiRestRouteInfo(root, name, wrapper.controllerType, caps, subRouteResources)
  def subRouteResources = routeMap.map { t => t._2.routeInfo(t._1) }.toSeq

  def add(t: (String, Routing[C])): this.type = {
    routeMap = routeMap + t
    this
  }

  def add(route: String, router: SubRestResourceRouter[C, _]): this.type = add(route-> new SubResourceRouting(router))
  def add(router: SubRestResourceRouter[C, _]): this.type = add(router.name, router)

  def add(route: String, router: RestResourceRouter[_]): this.type = this.add(route-> new ResourceRouting(router))
  def add(router: RestResourceRouter[_]): this.type = add(router.name, router)

  def add[S<:BaseResource : ResourceWrapper](route: String, factory: ControllerFactory[C, S]): this.type =
    this.add(route, new SubRestResourceRouter(route, factory))

  def add(route: String, method: HttpMethod, action: ResourceAction[C]): this.type =
    this.add(route-> new ActionRouting(method, action, route))
}


class RestResourceRouter[C<:BaseResource: ResourceWrapper]( val controller: C,
                                                            var routeMap: Map[String, Routing[C]]= Map[String, Routing[C]](),
                                                            val parentContext: Option[RouteFilterContext[_]] = None)
      extends AbstractRestResourceRouter[C] with RestRouter with SimpleRouter{
  val name = controller.name

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
      def next(id: C#IdentifierType) = () =>
        action.routing(
          controller,
          id,
          requestHeader,
          requestHeader.path.take(prefixLength + subPrefix.length),
          RouteFilterContext(name, Some(sid), Some(id), parentContext)
        )
      wrapper.routeFilterWrapper.filterTraverse(controller, requestHeader, name, sid, parentContext, next)
    }
  }

  def routeRequest(requestHeader: RequestHeader, path: String, method: String): Option[Handler] = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath)

      case "" | "/" => method match {
        case "GET"     => wrapper.readWrapper.list(controller)
        case "POST"    => wrapper.createWrapper(controller)
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }

      case IdExpression(sid) => method match {
        case "GET"     => wrapper.readWrapper(controller, sid)
        case "PUT"     => wrapper.writeWrapper(controller, sid)
        case "DELETE"  => wrapper.deleteWrapper(controller, sid)
        case "PATCH"   => wrapper.updateWrapper(controller, sid)
        case "OPTIONS" => controller.parseId(sid).map(res => idOptionsRoutingHandler())
        case _         => controller.parseId(sid).map(res => methodNotAllowed)
      }

      case _ => None
    }
  }
}


class SubRestResourceRouter[P<:BaseResource, C <: BaseResource : ResourceWrapper](val name: String, val factory: ControllerFactory[P, C]) extends AbstractRestResourceRouter[C] {
  var routeMap = Map[String, Routing[C]]()
  override val caps = super.caps ++ ResourceCaps.ValueSet(ResourceCaps.Child)

  def withParent(parent: P, id: P#IdentifierType, context: RouteFilterContext[P#IdentifierType]) = new RestResourceRouter[C](
    factory.construct(parent, id),
    routeMap,
    Some(context)
  )
}
