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

class RestResourceRouter(val controller: Controller with Resource) extends RestRouter{

  protected var _prefix: String = ""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val _defaultVerifyId = (sid: String) => false
  private def _verifyId[R](resource: IdentifiedResource[R]) =
    (sid: String) => resource.fromId(sid).isDefined

  lazy val verifyId =
    if(controller.caps contains ResourceCaps.Identity)
      _verifyId(controller.asInstanceOf[IdentifiedResource[_]])
    else
      _defaultVerifyId

  private var methodNotAllowed = Action { Results.MethodNotAllowed }
  private val _defaultRoutingHandler = () => methodNotAllowed
  private val _defaultIdRoutingHandler = (sid: String) => if(verifyId(sid)) Some(methodNotAllowed) else None
  private val _defaultSubRoutingHandler = (requestHeader: RequestHeader, subPrefix: String, id: String, subPath: String) => None

  private def _getRoutingHandler[R](resource: ResourceRead[R]) =
    (sid: String) => resource.fromId(sid).map(resource.read)

  private def _listRoutingHandler[R](resource: ResourceRead[R]) = resource.list _

  private def _putRoutingHandler[R](resource: ResourceWrite[R]) =
    (sid: String) => resource.fromId(sid).map(resource.write)

  private def _deleteRoutingHandler[R](resource: ResourceDelete[R]) =
    (sid: String) => resource.fromId(sid).map(resource.delete)

  private def _patchRoutingHandler[R](resource: ResourceUpdate[R]) =
    (sid: String) => resource.fromId(sid).map(resource.update)

  private def _postRoutingHandler[R](resource: ResourceCreate) = resource.create _

  private def _subRoutingHandler[R](resource: SubResource[R]) =
    (requestHeader: RequestHeader, subPrefix: String, sid: String, subPath: String) => {
      for {
        action <- resource.subResources.get(subPath)
        id <- resource.fromId(sid)
        res <- action(id, requestHeader, requestHeader.path.take(_prefix.length()+subPrefix.length()))
      } yield res
    }

  lazy val getRoutingHandler = if(controller.caps contains ResourceCaps.Read)
    _getRoutingHandler(controller.asInstanceOf[ResourceRead[_]]) else _defaultIdRoutingHandler
  lazy val listRoutingHandler = if(controller.caps contains ResourceCaps.Read)
    _listRoutingHandler(controller.asInstanceOf[ResourceRead[_]]) else _defaultRoutingHandler
  lazy val putRoutingHandler = if(controller.caps contains ResourceCaps.Write)
     _putRoutingHandler(controller.asInstanceOf[ResourceWrite[_]]) else _defaultIdRoutingHandler
  lazy val patchRoutingHandler = if(controller.caps contains ResourceCaps.Update)
    _patchRoutingHandler(controller.asInstanceOf[ResourceUpdate[_]]) else _defaultIdRoutingHandler
  lazy val deleteRoutingHandler = if(controller.caps contains ResourceCaps.Delete)
    _deleteRoutingHandler(controller.asInstanceOf[ResourceDelete[_]]) else _defaultIdRoutingHandler
  lazy val postRoutingHandler = if(controller.caps contains ResourceCaps.Create)
    _postRoutingHandler(controller.asInstanceOf[ResourceCreate]) else _defaultRoutingHandler
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
  def idOptionsRoutingHandler(sid: String) = {
    if(verifyId(sid)){
      Some(optionsRoutingHandler(ID_OPTIONS))
    } else {
      None
    }
  }

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
            case "GET"     => listRoutingHandler()
            case "POST"    => postRoutingHandler()
            case "OPTIONS" => rootOptionsRoutingHandler()
            case _         => methodNotAllowed
          }
          case IdExpression(sid) => { method match {
              case "GET"    => getRoutingHandler(sid)
              case "PUT"    => putRoutingHandler(sid)
              case "DELETE" => deleteRoutingHandler(sid)
              case "PATCH"  => patchRoutingHandler(sid)
              case "OPTIONS" => idOptionsRoutingHandler(sid)
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

