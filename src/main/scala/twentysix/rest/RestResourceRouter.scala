package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions


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
trait DefaultReadResourceWrapper {
  implicit def defaultImpl[T] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = None
        def list(obj: T) = methodNotAllowed
  }
}
trait IdentifiedReadResourceWrapper extends DefaultReadResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
        def list(obj: T) = methodNotAllowed
  }
}
object ReadResourceWrapper extends IdentifiedReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResourceRead] = new ReadResourceWrapper[T]{
    def read(obj: T, sid: String) = obj.fromId(sid).map(obj.read(_))
    def list(obj: T): EssentialAction = obj.list
  }
}

trait WriteResourceWrapper[T] extends ResourceWrapperBase{
  def write(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultWriteResourceWrapper {
  implicit def defaultImpl[T] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = None
  }
}
trait IdentifiedWriteResourceWrapper extends DefaultWriteResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
}
object WriteResourceWrapper extends IdentifiedWriteResourceWrapper{
  implicit def writeResourceImpl[T<:BaseResourceWrite] = new WriteResourceWrapper[T]{
    def write(obj: T, sid: String) = obj.fromId(sid).map(obj.write(_))
  }
}


trait UpdateResourceWrapper[T] extends ResourceWrapperBase{
  def update(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultUpdateResourceWrapper {
  implicit def defaultImpl[T] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = None
  }
}
trait IdentifiedUpdateResourceWrapper extends DefaultUpdateResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
}
object UpdateResourceWrapper extends IdentifiedUpdateResourceWrapper{
  implicit def updateResourceImpl[T<:BaseResourceUpdate] = new UpdateResourceWrapper[T]{
    def update(obj: T, sid: String) = obj.fromId(sid).map(obj.update(_))
  }
}

trait DeleteResourceWrapper[T] extends ResourceWrapperBase{
  def delete(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultDeleteResourceWrapper{
  implicit def defaultImpl[T] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = None
  }
}
trait IdentifiedDeleteResourceWrapper extends DefaultDeleteResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseIdentifiedResource] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = obj.fromId(sid).map(_ => methodNotAllowed)
  }
}
object DeleteResourceWrapper extends IdentifiedDeleteResourceWrapper{
  implicit def deleteResourceImpl[T<:BaseResourceDelete] = new DeleteResourceWrapper[T]{
    def delete(obj: T, sid: String) = obj.fromId(sid).map(obj.delete(_))
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

trait RouteResourceWrapper[T] extends ResourceWrapperBase{
  def handleRoute(obj: T, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String): Option[Handler]
  def routeResources(obj: T, root: String): RestRouteInfo
}
trait DefaultRouteResourceWrapper {
  implicit def defaultImpl[T<:Resource] = new RouteResourceWrapper[T]{
    def handleRoute(obj: T, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = None
    def routeResources(obj: T, root: String) = RestRouteInfo(root, obj, Seq())
  }
}
object RouteResourceWrapper extends DefaultRouteResourceWrapper{
  implicit def createResourceImpl[T<:BaseResourceRoutes] = new RouteResourceWrapper[T]{
    def handleRoute(obj: T, requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = {
      for {
        action <- obj.routeMap.routeMap.get(subPath)
        id <- obj.fromId(sid)
        res <- action.routing(id, requestHeader, requestHeader.path.take(prefixLength+subPrefix.length()))
      } yield res
    }
    def routeResources(obj: T, root: String) = RestRouteInfo(root, obj, obj.routeMap.routeMap.map { t => t._2.routeInfo(t._1) }.toSeq )
  }
}

class RestResourceRouter[C<:Controller with Resource: IdentifiedResourceWrapper: ReadResourceWrapper: WriteResourceWrapper: UpdateResourceWrapper: DeleteResourceWrapper: CreateResourceWrapper: RouteResourceWrapper]
    (val controller: C) extends RestRouter with SimpleRouter{

  private val methodNotAllowed = Action { Results.MethodNotAllowed }
  private val IdExpression = "^/([^/]+)/?$".r
  private val SubResourceExpression = "^(/([^/]+)/([^/]+)).*$".r

  val identifiedResourceWrapper = implicitly[IdentifiedResourceWrapper[C]]
  val readResourceWrapper = implicitly[ReadResourceWrapper[C]]
  val writeResourceWrapper = implicitly[WriteResourceWrapper[C]]
  val updateResourceWrapper = implicitly[UpdateResourceWrapper[C]]
  val deleteResourceWrapper = implicitly[DeleteResourceWrapper[C]]
  val createResourceWrapper = implicitly[CreateResourceWrapper[C]]
  val routeResourceWrapper = implicitly[RouteResourceWrapper[C]]

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

  def routeResources(root: String) = Seq(routerRouteResource(root))
  def routerRouteResource(root: String) = routeResourceWrapper.routeResources(controller, root)

  def routeRequest(requestHeader: RequestHeader, path: String, method: String) = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        routeResourceWrapper.handleRoute(controller, requestHeader, _prefix.length(), subPrefix, id, subPath)
      case "" | "/" => method match {
        case "GET"     => Some(readResourceWrapper.list(controller))
        case "POST"    => Some(createResourceWrapper.create(controller))
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }
      case IdExpression(sid) => { method match {
        case "GET"     => readResourceWrapper.read(controller, sid)
        case "PUT"     => writeResourceWrapper.write(controller, sid)
        case "DELETE"  => deleteResourceWrapper.delete(controller, sid)
        case "PATCH"   => updateResourceWrapper.update(controller, sid)
        case "OPTIONS" => identifiedResourceWrapper.fromId(controller, sid).map(res => idOptionsRoutingHandler())
        case _         => identifiedResourceWrapper.fromId(controller, sid).map(res => methodNotAllowed)
      }}
      case _  => None
    }
  }
}

class SubRestResourceRouter[P, C<:Controller with SubResource[P, C]: IdentifiedResourceWrapper: ReadResourceWrapper: WriteResourceWrapper: UpdateResourceWrapper: DeleteResourceWrapper: CreateResourceWrapper: RouteResourceWrapper]
    (controller: C) extends RestResourceRouter(controller){
  def withParent(id: P) = new SubRestResourceRouter[P, C](controller.withParent(id))
}
