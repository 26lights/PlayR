package twentysix.rest

import play.core.Router
import play.api.mvc.EssentialAction
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Controller
import play.api.Logger
import play.api.mvc.Controller
import scala.reflect._

abstract class RestAction[Id] {
  def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler]

}
object RestAction {
  def apply[Id](f: Id => Controller) = new RestAction[Id] {
    def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val router = new RestRouter(f(id))
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
  }
  def apply[Id](method: String, f: Id => EssentialAction) = new RestAction[Id] {
    def apply(id: Id, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else None
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
  private val _defaultRoutingHandler = () => None
  private val _defaultIdRoutingHandler = (id: String) => None
  private val _defaultSubRoutingHandler = (requestHeader: RequestHeader, subPrefix: String, id: String, subPath: String) => None

  private def _getRoutingHandler[Id](resource: ResourceRead[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.get(_) }

  private def _listRoutingHandler[Id](resource: ResourceRead[Id]) =
    () => Some(resource.list)

  private def _putRoutingHandler[Id](resource: ResourceOverwrite[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.put(_) }

  private def _deleteRoutingHandler[Id](resource: ResourceDelete[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.delete(_) }

  private def _patchRoutingHandler[Id](resource: ResourceUpdate[Id]) =
    (sid: String) => resource.fromId(sid).map { resource.update(_) }

  private def _postRoutingHandler[Id](resource: ResourceCreate) =
    () => Some(resource.create)

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

  val getRoutingHandler = controllerAs[ResourceRead[_]].map( _getRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val listRoutingHandler = controllerAs[ResourceRead[_]].map( _listRoutingHandler(_)).getOrElse(_defaultRoutingHandler)
  val putRoutingHandler = controllerAs[ResourceOverwrite[_]].map( _putRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val patchRoutingHandler = controllerAs[ResourceUpdate[_]].map( _patchRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val deleteRoutingHandler = controllerAs[ResourceDelete[_]].map( _deleteRoutingHandler(_)).getOrElse(_defaultIdRoutingHandler)
  val postRoutingHandler = controllerAs[ResourceCreate].map( _postRoutingHandler(_)).getOrElse(_defaultRoutingHandler)
  val subRoutingHandler = controllerAs[SubResource[_]].map( _subRoutingHandler(_)).getOrElse(_defaultSubRoutingHandler)

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        val method = requestHeader.method

        val res = (method, path) match {
          case (_,        SubResourceExpression(subPrefix, id, subPath)) => subRoutingHandler(requestHeader, subPrefix, id, subPath)
          case ("GET",    "" | "/")         => listRoutingHandler()
          case ("GET",    IdExpression(id)) => getRoutingHandler(id)
          case ("POST",   "" | "/")         => postRoutingHandler()
          case ("PUT",    IdExpression(id)) => putRoutingHandler(id)
          case ("DELETE", IdExpression(id)) => deleteRoutingHandler(id)
          case ("PATCH",  IdExpression(id)) => patchRoutingHandler(id)
          case _  => None
        }
        res.getOrElse(default(requestHeader))
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
          case (IdExpression(_), "PUT", c: ResourceOverwrite[_]) => true
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

