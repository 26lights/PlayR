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

class ResourceWrapperGenerator[R, C<:Controller with Resource](val controller: C) {
  private var methodNotAllowed = Action { Results.MethodNotAllowed }

  def name = controller.name

  val fromId = if(controller.caps contains ResourceCaps.Identity)
    (sub: C, sid: String) => sub.asInstanceOf[IdentifiedResource[R]].fromId(sid)
    else (sub: C, sid: String) => None

  val read = if(controller.caps contains ResourceCaps.Read)
    (sub: C, id: R) => sub.asInstanceOf[ResourceRead[R]].read(id)
    else (sub: C, id: R) => methodNotAllowed

  val write = if(controller.caps contains ResourceCaps.Write)
    (sub: C, id: R) => sub.asInstanceOf[ResourceWrite[R]].write(id)
    else (sub: C, id: R) => methodNotAllowed

  val update = if(controller.caps contains ResourceCaps.Update)
    (sub: C, id: R) => sub.asInstanceOf[ResourceUpdate[R]].update(id)
    else (sub: C, id: R) => methodNotAllowed

  val delete = if(controller.caps contains ResourceCaps.Delete)
    (sub: C, id: R) => sub.asInstanceOf[ResourceDelete[R]].delete(id)
    else (sub: C, id: R) => methodNotAllowed

  val list = if(controller.caps contains ResourceCaps.Read)
    (sub: C) => sub.asInstanceOf[ResourceRead[R]].list() else (sub: C) => methodNotAllowed

  val create = if(controller.caps contains ResourceCaps.Create)
    (sub: C) => sub.asInstanceOf[ResourceCreate].create() else (sub: C) => methodNotAllowed

  def forController(subController: C) = {
    new ResourceWrapper (fromId, read, write, update, delete, list, create, subController)
  }

  class ResourceWrapper(val fromIdImpl: (C, String) => Option[R],
                        val readImpl: (C, R) => EssentialAction,
                        val writeImpl: (C, R) => EssentialAction,
                        val updateImpl: (C, R) => EssentialAction,
                        val deleteImpl:(C, R) => EssentialAction,
                        val listImpl: (C) => EssentialAction,
                        val createImpl: (C) => EssentialAction,
                        val subController: C)
      extends Resource
      with IdentifiedResource[R]
      with ResourceCreate
      with ResourceRead[R]
      with ResourceDelete[R]
      with ResourceWrite[R]
      with ResourceUpdate[R] {
    def name = subController.name
    def fromId(sid: String) = fromIdImpl(subController, sid)
    def read(id: R) = readImpl(subController, id)
    def write(id: R) = writeImpl(subController, id)
    def update(id: R) = updateImpl(subController, id)
    def delete(id: R) = deleteImpl(subController, id)
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
          case IdExpression(sid) => {
            resourceWrapper.fromId(sid).map { res =>
              method match {
                case "GET"    => resourceWrapper.read(res)
                case "PUT"    => resourceWrapper.write(res)
                case "DELETE" => resourceWrapper.delete(res)
                case "PATCH"  => resourceWrapper.update(res)
                case "OPTIONS" => idOptionsRoutingHandler()
                case _        => methodNotAllowed
              }
            }.getOrElse(default(requestHeader))
          }
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

