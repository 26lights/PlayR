package twentysix.rest

import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.runtime.AbstractPartialFunction
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import play.api.Logger
import scala.annotation.Annotation
import scala.annotation.ClassfileAnnotation
import scala.annotation.StaticAnnotation


sealed trait ResourceWrapperBase {
  val methodNotAllowed = Action { Results.MethodNotAllowed }
  val caps: ResourceCaps.ValueSet
}

sealed trait DefaultCaps{
  val caps = ResourceCaps.ValueSet.empty
}

sealed trait DefaultApply[T<:BaseResource] extends DefaultCaps {
  this: ResourceWrapperBase  =>
  def apply(obj: T, sid: String) = obj.requestWrapper(sid, obj.fromId(_).map(_ => methodNotAllowed))
}

trait ReadResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
  def list(obj: T): EssentialAction
}
trait DefaultReadResourceWrapper{
  implicit def identifiedResourceImpl[T<:BaseResource] = new ReadResourceWrapper[T] with DefaultApply[T]{
    def list(obj: T) = methodNotAllowed
  }
}
object ReadResourceWrapper extends DefaultReadResourceWrapper{
  implicit def readResourceImpl[T<:BaseResourceRead] = new ReadResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.readRequestWrapper(sid, obj.fromId(_).map(obj.read(_)))
    def list(obj: T): EssentialAction = obj.list
    val caps = ResourceCaps.ValueSet(ResourceCaps.Read)
  }
}

trait WriteResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultWriteResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new WriteResourceWrapper[T] with DefaultApply[T]
}
object WriteResourceWrapper extends DefaultWriteResourceWrapper{
  implicit def writeResourceImpl[T<:BaseResourceWrite] = new WriteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.writeRequestWrapper(sid, obj.fromId(_).map(obj.write(_)))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Write)
  }
}


trait UpdateResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultUpdateResourceWrapper {
  implicit def defaultImpl[T<:BaseResource] = new UpdateResourceWrapper[T] with DefaultApply[T]
}
object UpdateResourceWrapper extends DefaultUpdateResourceWrapper{
  implicit def updateResourceImpl[T<:BaseResourceUpdate] = new UpdateResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.updateRequestWrapper(sid, obj.fromId(_).map(obj.update(_)))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Update)
  }
}

trait DeleteResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T, sid: String): Option[EssentialAction]
}
trait DefaultDeleteResourceWrapper{
  implicit def defaultImpl[T<:BaseResource] = new DeleteResourceWrapper[T] with DefaultApply[T]
}
object DeleteResourceWrapper extends DefaultDeleteResourceWrapper{
  implicit def deleteResourceImpl[T<:BaseResourceDelete] = new DeleteResourceWrapper[T]{
    def apply(obj: T, sid: String) = obj.deleteRequestWrapper(sid, obj.fromId(_).map(obj.delete(_)))
    val caps = ResourceCaps.ValueSet(ResourceCaps.Delete)
  }
}

trait CreateResourceWrapper[T] extends ResourceWrapperBase{
  def apply(obj: T): EssentialAction
}
trait DefaultCreateResourceWrapper {
  implicit def defaultImpl[T] = new CreateResourceWrapper[T] with DefaultCaps{
    def apply(obj: T) = methodNotAllowed
  }
}
object CreateResourceWrapper extends DefaultCreateResourceWrapper{
  implicit def createResourceImpl[T<:BaseResourceCreate] = new CreateResourceWrapper[T]{
    def apply(obj: T) = obj.create
    val caps = ResourceCaps.ValueSet(ResourceCaps.Create)
  }
}

trait ResourceWrapper[T]{
  val readWrapper: ReadResourceWrapper[T]
  val writeWrapper: WriteResourceWrapper[T]
  val updateWrapper: UpdateResourceWrapper[T]
  val deleteWrapper: DeleteResourceWrapper[T]
  val createWrapper: CreateResourceWrapper[T]
  val controllerType: Type
}
object ResourceWrapper {
  implicit def resourceWrapperImpl[C<:BaseResource:
                                   TypeTag:
                                   ReadResourceWrapper:
                                   WriteResourceWrapper:
                                   UpdateResourceWrapper:
                                   DeleteResourceWrapper:
                                   CreateResourceWrapper] =
    new ResourceWrapper[C] {
    val readWrapper = implicitly[ReadResourceWrapper[C]]
    val writeWrapper = implicitly[WriteResourceWrapper[C]]
    val updateWrapper = implicitly[UpdateResourceWrapper[C]]
    val deleteWrapper = implicitly[DeleteResourceWrapper[C]]
    val createWrapper = implicitly[CreateResourceWrapper[C]]
    val controllerType = typeOf[C]
  }
}

sealed trait Routing[C<:BaseResource] {
  def routing(controller: C, id: C#ResourceType, requestHeader: RequestHeader, prefix: String): Option[Handler]
  def routeInfo(path: String): RestRouteInfo
}

abstract class AbstractRestResourceRouter[C<:BaseResource: ResourceWrapper] {
  val wrapper = implicitly[ResourceWrapper[C]]
  def caps = wrapper.readWrapper.caps ++
             wrapper.writeWrapper.caps ++
             wrapper.updateWrapper.caps ++
             wrapper.deleteWrapper.caps ++
             wrapper.createWrapper.caps ++ {
               if(routeMap.isEmpty) ResourceCaps.ValueSet.empty
               else ResourceCaps.ValueSet(ResourceCaps.Parent)
             }

  val name: String
  var routeMap: Map[String, Routing[C]]

  class SubResourceRouting(val router: SubRestResourceRouter[C#ResourceType, _]) extends Routing[C]{
    def routing(controller: C, id: C#ResourceType, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        val subRouter = router.withParent(id)
        subRouter.setPrefix(prefix)
        subRouter
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }

  class ResourceRouting(val router: RestResourceRouter[_]) extends Routing[C]{
    def routing(controller: C, id: C#ResourceType, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
      Router.Include {
        router.setPrefix(prefix)
        router
      }.unapply(requestHeader)
    }
    def routeInfo(path: String) = router.routerRouteResource(path)
  }


  def routeResources(root: String) = Seq(routerRouteResource(root))
  def routerRouteResource(root: String) = ApiRestRouteInfo(root, name, wrapper.controllerType, caps, subRouteResources)
  def subRouteResources = routeMap.map { t => t._2.routeInfo(t._1) }.toSeq

  def add(t: (String, Routing[C])): this.type = {
    routeMap = routeMap + t
    Logger.debug(s"t: $t  -  routeMap: $routeMap")
    this
  }

  def add(route: String, router: SubRestResourceRouter[C#ResourceType, _]): this.type = add(route-> new SubResourceRouting(router))
  def add(route: String, router: RestResourceRouter[_]): this.type = this.add(route-> new ResourceRouting(router))
}

class RestResourceRouter[C<:BaseResource: ResourceWrapper](val controller: C, var routeMap: Map[String, Routing[C]]= Map[String, Routing[C]]())
      extends AbstractRestResourceRouter[C] with RestRouter with SimpleRouter{
  val name = controller.name

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
    val options = map.filterKeys( caps contains _).values mkString ","
    Results.Ok.withHeaders(ALLOW -> options)
  }

  def rootOptionsRoutingHandler = optionsRoutingHandler(ROOT_OPTIONS)
  def idOptionsRoutingHandler = optionsRoutingHandler(ID_OPTIONS)

  def handleRoute(requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String) = {
    Logger.debug(s"subPath=$subPath routeMap=$routeMap")
    for {
      action <- routeMap.get(subPath)
      id <- controller.fromId(sid)
      res <- action.routing(controller, id, requestHeader, requestHeader.path.take(prefixLength+subPrefix.length()))
    } yield res
  }

  def routeRequest(requestHeader: RequestHeader, path: String, method: String) = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath)
      case "" | "/" => method match {
        case "GET"     => Some(wrapper.readWrapper.list(controller))
        case "POST"    => Some(wrapper.createWrapper(controller))
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }
      case IdExpression(sid) => { method match {
        case "GET"     => wrapper.readWrapper(controller, sid)
        case "PUT"     => wrapper.writeWrapper(controller, sid)
        case "DELETE"  => wrapper.deleteWrapper(controller, sid)
        case "PATCH"   => wrapper.updateWrapper(controller, sid)
        case "OPTIONS" => controller.fromId(sid).map(res => idOptionsRoutingHandler())
        case _         => controller.fromId(sid).map(res => methodNotAllowed)
      }}
      case _  => None
    }
  }

  class ActionRouting[F<:EssentialAction:TypeTag](val method: String, val f: Function1[C#ResourceType, F], route: String) extends Routing[C] {
    def routing(controller: C, id: C#ResourceType, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(id))
        else Some(Action { Results.MethodNotAllowed })
    }
    def routeInfo(path: String) = ActionRestRouteInfo(path, route, typeOf[F], ResourceCaps.ValueSet(ResourceCaps.Action), Seq(), method)
  }

  def add[F<:EssentialAction:TypeTag](route: String, method: String, f: Function1[C#ResourceType, F]): this.type =
    this.add(route-> new ActionRouting(method, f, route))
}

class SubRestResourceRouter[P, C<:BaseResource: ResourceWrapper](val name: String, val factory: P => C) extends AbstractRestResourceRouter[C]{
  var routeMap = Map[String, Routing[C]]()
  override val caps = super.caps ++ ResourceCaps.ValueSet(ResourceCaps.Child)
  def withParent(id: P) = new RestResourceRouter[C](factory(id), routeMap)

  class ActionRouting[F<:EssentialAction:TypeTag](val method: String, val f: C => Function1[C#ResourceType, F], route: String) extends Routing[C] {
    def routing(controller: C, id: C#ResourceType, requestHeader: RequestHeader, prefix: String): Option[Handler] = {
        if (method==requestHeader.method) Some(f(controller)(id))
        else Some(Action { Results.MethodNotAllowed })
    }
    def routeInfo(path: String) = ActionRestRouteInfo(path, route, typeOf[F], ResourceCaps.ValueSet(ResourceCaps.Action), Seq(), method)
  }

  def add[F<:EssentialAction:TypeTag](route: String, method: String, f: C => Function1[C#ResourceType, F]): this.type =
    this.add(route-> new ActionRouting(method, f, route))
}
