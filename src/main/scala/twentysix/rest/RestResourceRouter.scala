package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions

case class ResourceRouteMap[R](routeMap: Map[String, (R, RequestHeader, String)=> Option[Handler]] = Map[String, (R, RequestHeader, String)=> Option[Handler]]()) {
  sealed trait Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler]
  }

  class ResourceRouting(val router: RestResourceRouter) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }

  class ControllerRouting[C<:Controller with SubResource[R, C]](val controller: C) extends Routing{
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestResourceRouter(controller.withParent(id))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }

  class ActionRouting(val method: String, val f: R => EssentialAction) extends Routing {
    def routing(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
  }


  def add(t: (String, (R, RequestHeader, String)=> Option[Handler])) = this.copy(routeMap = this.routeMap + t )

  def add(route: String, router: RestResourceRouter): ResourceRouteMap[R] = this.add(route-> new ResourceRouting(router).routing _)
  def add[C<:Controller with SubResource[R, C]](route: String, controller: C): ResourceRouteMap[R] =
    this.add(route-> new ControllerRouting(controller).routing _)
  def add(route: String, method: String, f: (R => EssentialAction)): ResourceRouteMap[R] =
    this.add(route-> new ActionRouting(method, f).routing _)

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
      res <- action(id, requestHeader, requestHeader.path.take(prefixLength+subPrefix.length()))
    } yield res
  }
  private def _defaultHandleRoute(sub: C, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = None
  val handleRoute = if(controller.caps contains ResourceCaps.Parent) _handleRoute _ else _defaultHandleRoute _

  val list = if(controller.caps contains ResourceCaps.Read)
    (sub: C) => sub.asInstanceOf[ResourceRead[_]].list() else (sub: C) => methodNotAllowed

  val create = if(controller.caps contains ResourceCaps.Create)
    (sub: C) => sub.asInstanceOf[ResourceCreate].create() else (sub: C) => methodNotAllowed

  def forController(subController: C) = {
    new ResourceWrapper (fromId, read, write, update, delete, list, create, handleRoute, subController)
  }

  class ResourceWrapper(val fromIdImpl: (C, String) => Option[_],
                        val readImpl: (C, String) => Option[EssentialAction],
                        val writeImpl: (C, String) => Option[EssentialAction],
                        val updateImpl: (C, String) => Option[EssentialAction],
                        val deleteImpl:(C, String) => Option[EssentialAction],
                        val listImpl: (C) => EssentialAction,
                        val createImpl: (C) => EssentialAction,
                        val handleRouteImpl: (C, RequestHeader, Int, String, String, String) => Option[Handler],
                        val subController: C) {
    def name = subController.name
    def fromId(sid: String) = fromIdImpl(subController, sid)
    def read(sid: String) = readImpl(subController, sid)
    def write(sid: String) = writeImpl(subController, sid)
    def update(sid: String) = updateImpl(subController, sid)
    def delete(sid: String) = deleteImpl(subController, sid)
    def create() = createImpl(subController)
    def list() = listImpl(subController)
    def handleRoute(requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) =
      handleRouteImpl(subController, requestHeader, prefixLength, subPrefix, sid, subPath)
  }
}


class RestResourceRouter(val controller: Controller with Resource) extends RestRouter{

  protected var _prefix: String = ""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private val methodNotAllowed = Action { Results.MethodNotAllowed }
  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val resourceWrapperGenerator = new ResourceWrapperGenerator(controller)
  private val resourceWrapper = resourceWrapperGenerator.forController(controller)

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
    val options = map.filterKeys( controller.caps contains _).values mkString ","
    Results.Ok.withHeaders(ALLOW -> options)
  }

  def rootOptionsRoutingHandler = optionsRoutingHandler(ROOT_OPTIONS)
  def idOptionsRoutingHandler = optionsRoutingHandler(ID_OPTIONS)

  def routeResources = Map("" -> controller)

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

        (path, method, controller.caps) match {
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
