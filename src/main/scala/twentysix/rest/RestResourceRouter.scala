package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction

abstract class RestPath[R] {
  def apply(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler]

}
object RestPath {
  def apply[R](router: RestResourceRouter) = new RestPath[R] {
    def apply(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }
  def apply[R](f: R => Controller with Resource) = new RestPath[R] {
    def apply(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestResourceRouter(f(id))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }
  def apply[R](method: String, f: R => EssentialAction) = new RestPath[R] {
    def apply(id: R, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
  }
}

class ResourceWrapperGenerator[C<:Controller with Resource](val controller: C) {
  private var methodNotAllowed = Action { Results.MethodNotAllowed }

  def name = controller.name

  val fromId = if(controller.caps contains ResourceCaps.Identity)
    (sub: C, sid: String) => sub.asInstanceOf[IdentifiedResource[_]].fromId(sid)
    else (sub: C, sid: String) => None

  def _read[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceRead[R]]
    ctrl.fromId(sid).map(ctrl.read(_))
  }
  val read = if(controller.caps contains ResourceCaps.Read) _read _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _write[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceWrite[R]]
    ctrl.fromId(sid).map(ctrl.write(_))
  }
  val write = if(controller.caps contains ResourceCaps.Write) _write _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _update[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceUpdate[R]]
    ctrl.fromId(sid).map(ctrl.update(_))
  }
  val update = if(controller.caps contains ResourceCaps.Update) _update _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _delete[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceDelete[R]]
    ctrl.fromId(sid).map(ctrl.delete(_))
  }
  val delete = if(controller.caps contains ResourceCaps.Delete) _delete _  else (sub: C, sid: String) => Some(methodNotAllowed)

  val list = if(controller.caps contains ResourceCaps.Read)
    (sub: C) => sub.asInstanceOf[ResourceRead[_]].list() else (sub: C) => methodNotAllowed

  val create = if(controller.caps contains ResourceCaps.Create)
    (sub: C) => sub.asInstanceOf[ResourceCreate].create() else (sub: C) => methodNotAllowed

  def forController(subController: C) = {
    new ResourceWrapper (fromId, read, write, update, delete, list, create, subController)
  }

  class ResourceWrapper(val fromIdImpl: (C, String) => Option[_],
                        val readImpl: (C, String) => Option[EssentialAction],
                        val writeImpl: (C, String) => Option[EssentialAction],
                        val updateImpl: (C, String) => Option[EssentialAction],
                        val deleteImpl:(C, String) => Option[EssentialAction],
                        val listImpl: (C) => EssentialAction,
                        val createImpl: (C) => EssentialAction,
                        val subController: C) {
    def name = subController.name
    def fromId(sid: String) = fromIdImpl(subController, sid)
    def read(sid: String) = readImpl(subController, sid)
    def write(sid: String) = writeImpl(subController, sid)
    def update(sid: String) = updateImpl(subController, sid)
    def delete(sid: String) = deleteImpl(subController, sid)
    def create() = createImpl(subController)
    def list() = listImpl(subController)
  }
}


class RestResourceRouter(val controller: Controller with Resource) extends RestRouter{

  protected var _prefix: String = ""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private var methodNotAllowed = Action { Results.MethodNotAllowed }
  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val resourceWrapperGenerator = new ResourceWrapperGenerator(controller)
  private val resourceWrapper = resourceWrapperGenerator.forController(controller)

  private val _defaultSubRoutingHandler = (requestHeader: RequestHeader, subPrefix: String, id: String, subPath: String) => None
  private def _subRoutingHandler[R](resource: SubResource[R]) =
    (requestHeader: RequestHeader, subPrefix: String, sid: String, subPath: String) => {
      for {
        action <- resource.subResources.get(subPath)
        id <- resource.fromId(sid)
        res <- action(id, requestHeader, requestHeader.path.take(_prefix.length()+subPrefix.length()))
      } yield res
    }
  lazy val subRoutingHandler = if(controller.caps contains ResourceCaps.Child)
    _subRoutingHandler(controller.asInstanceOf[SubResource[_]]) else _defaultSubRoutingHandler

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
            subRoutingHandler(requestHeader, subPrefix, id, subPath).getOrElse(default(requestHeader))
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

