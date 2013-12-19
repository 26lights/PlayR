package twentysix.rest

import play.core.Router
import play.api.mvc._
import scala.runtime.AbstractPartialFunction
import scala.reflect._

abstract class RestPath[Id] {
  def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler]

}
object RestPath {
  def apply[Id](f: Id => Controller) = new RestPath[Id] {
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

class RestRouter(val controller: Controller) extends Router.Routes {

  protected var _prefix: String =""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation= Nil

  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  val f = (id:String) => println(id)
  private var methodNotAllowed = Action { Results.MethodNotAllowed }
  private val _defaultVerifyId = (sid: String) => false
  private val _defaultRoutingHandler = () => methodNotAllowed
  private val _defaultIdRoutingHandler = (sid: String) => if(verifyId(sid)) Some(methodNotAllowed) else None
  private val _defaultSubRoutingHandler = (requestHeader: RequestHeader, subPrefix: String, id: String, subPath: String) => None

  private def _verifyId[Id](resource: IdentifiedResource[Id]) =
    (sid: String) => resource.fromId(sid).isDefined

  private def _getRoutingHandler[Id](resource: ResourceRead[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.get(_) }

  private def _listRoutingHandler[Id](resource: ResourceRead[Id]) =
    () => resource.list

  private def _putRoutingHandler[Id](resource: ResourceWrite[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.write(_) }

  private def _deleteRoutingHandler[Id](resource: ResourceDelete[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.delete(_) }

  private def _patchRoutingHandler[Id](resource: ResourceUpdate[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.update(_) }

  private def _postRoutingHandler[Id](resource: ResourceCreate) =
    () => resource.create

  private def _subRoutingHandler[Id](resource: SubResource[Id]) =
    (requestHeader: RequestHeader, subPrefix: String, sid: String, subPath: String) => {
      for {
        action <- resource.subResources.get(subPath)
        id <- resource.fromId(sid)
        res <- action(id, requestHeader, requestHeader.path.take(_prefix.length()+subPrefix.length()))
      } yield res
    }

  private def controllerAs[A: ClassTag]: Option[A] = {
    if(classTag[A].runtimeClass.isInstance(controller)) {
      Some(controller.asInstanceOf[A])
    } else {
      None
    }
  }

  val verifyId = controllerAs[IdentifiedResource[_]].map( _verifyId(_)).getOrElse(_defaultVerifyId)
  val getRoutingHandler = controllerAs[ResourceRead[_]].map( _getRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val listRoutingHandler = controllerAs[ResourceRead[_]].map( _listRoutingHandler(_)).getOrElse(_defaultRoutingHandler)
  val putRoutingHandler = controllerAs[ResourceWrite[_]].map( _putRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val patchRoutingHandler = controllerAs[ResourceUpdate[_]].map( _patchRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val deleteRoutingHandler = controllerAs[ResourceDelete[_]].map( _deleteRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val postRoutingHandler = controllerAs[ResourceCreate].map( _postRoutingHandler(_)).getOrElse(_defaultRoutingHandler)
  val subRoutingHandler = controllerAs[SubResource[_]].map( _subRoutingHandler(_)).getOrElse(_defaultSubRoutingHandler)

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        val method = requestHeader.method

        path match {
          case SubResourceExpression(subPrefix, id, subPath) =>
            subRoutingHandler(requestHeader, subPrefix, id, subPath).getOrElse(default(requestHeader))
          case "" | "/" => method match {
            case "GET"  => listRoutingHandler()
            case "POST" => postRoutingHandler()
            case _      => methodNotAllowed
          }
          case IdExpression(sid) => { method match {
              case "GET"    => getRoutingHandler(sid)
              case "PUT"    => putRoutingHandler(sid)
              case "DELETE" => deleteRoutingHandler(sid)
              case "PATCH"  => patchRoutingHandler(sid)
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

        (path, method, controller) match {
          case (SubResourceExpression(_, _, _), _, c:SubResource[_]) => true
          case (_, "GET", c:ResourceRead[_]) => true
          case (_, "POST", c:ResourceCreate) => true
          case (IdExpression(_), "PUT", c: ResourceWrite[_]) => true
          case (IdExpression(_), "DELETE", c:ResourceUpdate[_]) => true
          case (IdExpression(_), "PATCH", c:ResourceUpdate[_]) => true
          case _     => false
        }
      } else {
        false
      }
    }
  }
}

