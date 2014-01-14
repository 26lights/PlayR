package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions

case class ResourceRouteMap[R](routeMap: Map[String, ResourceRouteMap[R]#Routing] = Map[String, ResourceRouteMap[R]#Routing]()) {
  sealed trait Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler]
    def resource: Resource
  }

  class ResourceRouting[C<:Controller with Resource](val router: RestResourceRouter[C]) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def resource = router.resourceWrapper.wrappedController
  }

  class ControllerRouting[C<:Controller with SubResource[R, C]](val controller: C) extends Routing{
    val resourceWrapperGenerator = new ResourceWrapperGenerator(controller)
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestResourceRouter(resourceWrapperGenerator.forController(controller.withParent(id)))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def resource = controller
  }

  class ActionRouting(val method: String, val f: R => EssentialAction) extends Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
    def resource = new Resource{
      val name = method
      caps += ResourceCaps.Action
    }
  }


  def add(t: (String, ResourceRouteMap[R]#Routing)) = this.copy(routeMap = this.routeMap + t )

  def add[C<:Controller with Resource](route: String, router: RestResourceRouter[C]): ResourceRouteMap[R] =
    this.add(route-> new ResourceRouting(router))
  def add[C<:Controller with SubResource[R, C]](route: String, controller: C): ResourceRouteMap[R] =
    this.add(route-> new ControllerRouting(controller))
  def add(route: String, method: String, f: (R => EssentialAction)): ResourceRouteMap[R] =
    this.add(route-> new ActionRouting(method, f))

}

class ResourceWrapperGenerator[C<:Controller with Resource](val controller: C) {
  private var methodNotAllowed = Action { Results.MethodNotAllowed }

  def name = controller.name

  val fromId = if(controller.caps contains ResourceCaps.Identity)
    (sub: C, sid: String) => sub.asInstanceOf[IdentifiedResource[_]].fromId(sid)
    else (sub: C, sid: String) => None

  def _read[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceRead[R]]
    ctrl.readRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.read(_)) })
  }
  val read = if(controller.caps contains ResourceCaps.Read) _read _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _write[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceWrite[R]]
    ctrl.writeRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.write(_)) })
  }
  val write = if(controller.caps contains ResourceCaps.Write) _write _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _update[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceUpdate[R]]
    ctrl.updateRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.update(_)) })
  }
  val update = if(controller.caps contains ResourceCaps.Update) _update _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _delete[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceDelete[R]]
    ctrl.deleteRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.delete(_)) })
  }
  val delete = if(controller.caps contains ResourceCaps.Delete) _delete _  else (sub: C, sid: String) => Some(methodNotAllowed)

  def _handleRoute[R](sub: C, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = {
    val ctrl = sub.asInstanceOf[ResourceRoutes[R]]
    for {
      action <- ctrl.routeMap.routeMap.get(subPath)
      id <- ctrl.fromId(sid)
      res <- action.routing(id, requestHeader, requestHeader.path.take(prefixLength+subPrefix.length()))
    } yield res
  }
  private def _defaultHandleRoute(sub: C, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = None
  val handleRoute = if(controller.caps contains ResourceCaps.Parent) _handleRoute _ else _defaultHandleRoute _

  def _routeResources[R](sub: C, prefix: String): Map[String, Resource] = {
    val ctrl = sub.asInstanceOf[ResourceRoutes[R]]
    ctrl.routeMap.routeMap.map { t =>
      val (route, routing) = t
      prefix+route -> routing.resource
    }
  }
  def _defaultRouteResources(sub: C, prefix: String): Map[String, Resource] = Map[String, Resource]()
  val routeResources = if(controller.caps contains ResourceCaps.Parent) _routeResources _ else _defaultRouteResources _

  val list = if(controller.caps contains ResourceCaps.Read)
    (sub: C) => sub.asInstanceOf[ResourceRead[_]].list() else (sub: C) => methodNotAllowed

  val create = if(controller.caps contains ResourceCaps.Create)
    (sub: C) => sub.asInstanceOf[ResourceCreate].create() else (sub: C) => methodNotAllowed

  def forController(subController: C) = {
    new ResourceWrapper (fromId, read, write, update, delete, list, create, handleRoute, routeResources, subController)
  }

  class ResourceWrapper(val fromIdImpl: (C, String) => Option[_],
                        val readImpl: (C, String) => Option[EssentialAction],
                        val writeImpl: (C, String) => Option[EssentialAction],
                        val updateImpl: (C, String) => Option[EssentialAction],
                        val deleteImpl:(C, String) => Option[EssentialAction],
                        val listImpl: (C) => EssentialAction,
                        val createImpl: (C) => EssentialAction,
                        val handleRouteImpl: (C, RequestHeader, Int, String, String, String) => Option[Handler],
                        val routeResourcesImpl: (C, String) => Map[String, Resource],
                        val wrappedController: C) {
    def name = wrappedController.name
    def fromId(sid: String) = fromIdImpl(wrappedController, sid)
    def read(sid: String) = readImpl(wrappedController, sid)
    def write(sid: String) = writeImpl(wrappedController, sid)
    def update(sid: String) = updateImpl(wrappedController, sid)
    def delete(sid: String) = deleteImpl(wrappedController, sid)
    def create() = createImpl(wrappedController)
    def list() = listImpl(wrappedController)
    def handleRoute(requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) =
      handleRouteImpl(wrappedController, requestHeader, prefixLength, subPrefix, sid, subPath)
    def routeResources(prefix: String) = routeResourcesImpl(wrappedController, prefix)
  }
}


class RestResourceRouter[C<:Controller with Resource](val resourceWrapper: ResourceWrapperGenerator[C]#ResourceWrapper) extends RestRouter{

  protected var _prefix: String = ""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

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
    val options = map.filterKeys( resourceWrapper.wrappedController.caps contains _).values mkString ","
    Results.Ok.withHeaders(ALLOW -> options)
  }

  def rootOptionsRoutingHandler = optionsRoutingHandler(ROOT_OPTIONS)
  def idOptionsRoutingHandler = optionsRoutingHandler(ID_OPTIONS)

  def routeResources = Map("" -> resourceWrapper.wrappedController)++resourceWrapper.routeResources(s"/:${resourceWrapper.wrappedController.name}_id/")

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        val method = requestHeader.method

        path match {
          case SubResourceExpression(subPrefix, id, subPath) =>
            resourceWrapper.handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath).getOrElse(default(requestHeader))
          case "" | "/" => method match {
            case "GET"     => resourceWrapper.list()
            case "POST"    => resourceWrapper.create()
            case "OPTIONS" => rootOptionsRoutingHandler()
            case _         => methodNotAllowed
          }
          case IdExpression(sid) => { method match {
            case "GET"    => resourceWrapper.read(sid)
            case "PUT"    => resourceWrapper.write(sid)
            case "DELETE" => resourceWrapper.delete(sid)
            case "PATCH"  => resourceWrapper.update(sid)
            case "OPTIONS" => resourceWrapper.fromId(sid).map(res => idOptionsRoutingHandler())
            case _        => Some(methodNotAllowed)
          }}.getOrElse(default(requestHeader))
          case _  => default(requestHeader)
        }
      } else {
        default(requestHeader)
      }
    }

    def isDefinedAt(requestHeader: RequestHeader): Boolean = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        val method = requestHeader.method

        (path, method, resourceWrapper.wrappedController.caps) match {
          case (SubResourceExpression(_, _, _), _, caps) if caps contains ResourceCaps.Child => true
          case (_, "GET", caps) if caps contains ResourceCaps.Read => true
          case (_, "POST", caps) if caps contains ResourceCaps.Create => true
          case (IdExpression(_), "PUT", caps) if caps contains ResourceCaps.Write => true
          case (IdExpression(_), "DELETE", caps) if caps contains ResourceCaps.Delete => true
          case (IdExpression(_), "PATCH", caps) if caps contains ResourceCaps.Update => true
          case _     => false
        }
      } else {
        false
      }
    }
  }
}

object RestResourceRouter {
  def apply(controller: Controller with Resource) = new RestResourceRouter(new ResourceWrapperGenerator(controller).forController(controller))
}
