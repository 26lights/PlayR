package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions

class ResourceWrapperGenerator[C<:Controller with Resource](val controller: C) {
  private var methodNotAllowed = Action { Results.MethodNotAllowed }

  def name = controller.name

  val fromId = if(controller.caps contains ResourceCaps.Identity)
    (sub: C, sid: String) => sub.asInstanceOf[IdentifiedResource[_]].fromId(sid)
    else (sub: C, sid: String) => None

  def _read[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceRead[R]]
    ctrl.readRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.read) })
  }
  val read = if(controller.caps contains ResourceCaps.Read) _read _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _write[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceWrite[R]]
    ctrl.writeRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.write) })
  }
  val write = if(controller.caps contains ResourceCaps.Write) _write _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _update[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceUpdate[R]]
    ctrl.updateRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.update) })
  }
  val update = if(controller.caps contains ResourceCaps.Update) _update _ else (sub: C, sid: String) => Some(methodNotAllowed)

  def _delete[R](sub: C, sid: String) = {
    val ctrl=sub.asInstanceOf[ResourceDelete[R]]
    ctrl.deleteRequestWrapper(sid, { _sid => ctrl.fromId(_sid).map(ctrl.delete) })
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

  def _routeResources[R](sub: C, path: String): RestRouteInfo = {
    val ctrl = sub.asInstanceOf[ResourceRoutes[R]]
    RestRouteInfo(path, controller, ctrl.routeMap.routeMap.map { t => t._2.routeInfo(t._1) }.toSeq )
  }
  def _defaultRouteResources(sub: C, path: String): RestRouteInfo = RestRouteInfo(path, controller, Seq())
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
                        val routeResourcesImpl: (C, String) => RestRouteInfo,
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
    def routeResources(path: String) = routeResourcesImpl(wrappedController, path)
  }
}

trait ResourceWrapperBase {
  val methodNotAllowed = Action { Results.MethodNotAllowed }
}

trait IdentifiedResourceWrapper[T] {
  type ResultType
  def fromId(obj: T, sid: String): Option[ResultType]
}
trait IdentifiedResourceWrapperDefault {
  implicit def defaultImpl[T] = new IdentifiedResourceWrapper[T]{
    type ResultType = Any
    def fromId(obj: T, sid: String) = None
  }
}
object IdentifiedResourceWrapper extends IdentifiedResourceWrapperDefault{
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new IdentifiedResourceWrapper[T]{
    type ResultType = T#ResourceType
    def fromId(obj: T, sid: String) = obj.fromId(sid)
  }
}

trait ReadResourceWrapper[T] extends ResourceWrapperBase{
  def read(obj: T, sid: String): Option[EssentialAction]
  def list(obj: T): EssentialAction
}
object ReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResourceRead] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = obj.fromId(sid).map(obj.read(_))
    def list(obj: T): EssentialAction = obj.list
  }
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
    def list(obj: T) = methodNotAllowed
  }
  implicit def defaultImpl[T] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = None
    def list(obj: T) = methodNotAllowed
  }
}

trait WriteResourceWrapper[T] extends ResourceWrapperBase{
  def write(obj: T, sid: String): Option[EssentialAction]
}
object WriteResourceWrapper{
  implicit def writeResourceImpl[T<:BaseResourceWrite] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = obj.fromId(sid).map(obj.write(_))
  }
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
  implicit def defaultImpl[T] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = None
  }
}

trait UpdateResourceWrapper[T] extends ResourceWrapperBase{
  def update(obj: T, sid: String): Option[EssentialAction]
}
object UpdateResourceWrapper{
  implicit def updateResourceImpl[T<:BaseResourceUpdate] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = obj.fromId(sid).map(obj.update(_))
  }
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
  implicit def defaultImpl[T] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = None
  }
}

trait DeleteResourceWrapper[T] extends ResourceWrapperBase{
  def delete(obj: T, sid: String): Option[EssentialAction]
}
object DeleteResourceWrapper{
  implicit def deleteResourceImpl[T<:BaseResourceDelete] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = obj.fromId(sid).map(obj.delete(_))
  }
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
  implicit def defaultImpl[T] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = None
  }
}

trait CreateResourceWrapper[T] extends ResourceWrapperBase{
  def create(obj: T): EssentialAction
}
object CreateResourceWrapper{
  implicit def createResourceImpl[T<:ResourceCreate] = new CreateResourceWrapper[T]{
    def create(obj: T) = obj.create
  }
  implicit def defaultImpl[T] = new CreateResourceWrapper[T]{
    def create(obj: T) = methodNotAllowed
  }
}


class RestResourceRouter[C<:Controller with Resource: IdentifiedResourceWrapper]
(val resourceWrapper: ResourceWrapperGenerator[C]#ResourceWrapper) extends RestRouter with SimpleRouter{

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

  def routeResources(root: String) = Seq(resourceWrapper.routeResources(root))

  def routeRequest(requestHeader: RequestHeader, path: String, method: String) = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        resourceWrapper.handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath)
      case "" | "/" => method match {
        case "GET"     => Some(resourceWrapper.list())
        case "POST"    => Some(resourceWrapper.create())
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }
      case IdExpression(sid) => { method match {
        case "GET"     => resourceWrapper.read(sid)
        case "PUT"     => resourceWrapper.write(sid)
        case "DELETE"  => resourceWrapper.delete(sid)
        case "PATCH"   => resourceWrapper.update(sid)
        case "OPTIONS" => resourceWrapper.fromId(sid).map(res => idOptionsRoutingHandler())
        case _         => resourceWrapper.fromId(sid).map(res => methodNotAllowed)
      }}
      case _  => None
    }
  }
}

object RestResourceRouter {
  def apply(controller: Controller with Resource) = new RestResourceRouter(new ResourceWrapperGenerator(controller).forController(controller))
}
