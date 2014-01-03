package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction

abstract class RestPath[Id] {
  def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler]

}
object RestPath {
  def apply[Id](router: RestRouter) = new RestPath[Id] {
    def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }
  def apply[Id](f: Id => Controller with Resource) = new RestPath[Id] {
    def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestRouter(f(id))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }
  def apply[Id](method: String, f: Id => EssentialAction) = new RestPath[Id] {
    def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
  }
}

class RestRouter(val controller: Controller with Resource) extends Router.Routes {

  protected var _prefix: String =""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation= Nil

  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  private val _defaultVerifyId = (sid: String) => false
  private def _verifyId[Id](resource: IdentifiedResource[Id]) =
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

  private def _getRoutingHandler[Id](resource: ResourceRead[Id]) =
    (sid: String) => resource.fromId(sid).map(resource.get)

  private def _listRoutingHandler[Id](resource: ResourceRead[Id]) = resource.list _

  private def _putRoutingHandler[Id](resource: ResourceWrite[Id]) =
    (sid: String) => resource.fromId(sid).map(resource.write)

  private def _deleteRoutingHandler[Id](resource: ResourceDelete[Id]) =
    (sid: String) => resource.fromId(sid).map(resource.delete)

  private def _patchRoutingHandler[Id](resource: ResourceUpdate[Id]) =
    (sid: String) => resource.fromId(sid).map(resource.update)

  private def _postRoutingHandler[Id](resource: ResourceCreate) = resource.create _

  private def _subRoutingHandler[Id](resource: SubResource[Id]) =
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

