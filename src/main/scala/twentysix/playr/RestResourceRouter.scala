package twentysix.playr

import scala.language.reflectiveCalls
import play.core.Router
import play.api.mvc._
import play.api.http.HeaderNames.ALLOW
import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.annotation.Annotation
import scala.annotation.ClassfileAnnotation
import scala.annotation.StaticAnnotation
import core.BaseResource


trait Routing[C<:BaseResource] {
  def routing(controller: C, id: C#IdentifierType, requestHeader: RequestHeader, prefix: String): Option[Handler]
  def parseId(sid: String): Option[C#IdentifierType]
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

  def routeResources(root: String) = Seq(routerRouteResource(root))
  def routerRouteResource(root: String) = ApiRestRouteInfo(root, name, wrapper.controllerType, caps, subRouteResources)
  def subRouteResources = routeMap.map { t => t._2.routeInfo(t._1) }.toSeq

  def add(t: (String, Routing[C])): this.type = {
    routeMap = routeMap + t
    this
  }
  def add(route: String, routing: Routing[C]): this.type = add(route -> routing)
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
    val options = map.filterKeys( caps.contains ).values mkString ","
    Results.Ok.withHeaders(ALLOW -> options)
  }

  def rootOptionsRoutingHandler = optionsRoutingHandler(ROOT_OPTIONS)
  def idOptionsRoutingHandler = optionsRoutingHandler(ID_OPTIONS)

  def handleRoute(requestHeader: RequestHeader, prefixLength: Int, subPrefix: String, sid: String, subPath: String): Option[Handler] = {
    for {
      action <- routeMap.get(subPath)
      id <- action.parseId(sid)
      res <- action.routing(
        controller,
        id,
        requestHeader,
        requestHeader.path.take(prefixLength + subPrefix.length)
      )
    } yield res
  }

  def routeRequest(requestHeader: RequestHeader, path: String, method: String): Option[Handler] = {
    path match {
      case SubResourceExpression(subPrefix, id, subPath) =>
        handleRoute(requestHeader, _prefix.length(), subPrefix, id, subPath)

      case "" | "/" => method match {
        case "GET"     => Some(wrapper.readWrapper.list(controller))
        case "POST"    => Some(wrapper.createWrapper(controller))
        case "OPTIONS" => Some(rootOptionsRoutingHandler())
        case _         => Some(methodNotAllowed)
      }

      case IdExpression(sid) => method match {
        case "GET"     => wrapper.readWrapper(controller, sid)
        case "PUT"     => wrapper.writeWrapper(controller, sid)
        case "DELETE"  => wrapper.deleteWrapper(controller, sid)
        case "PATCH"   => wrapper.updateWrapper(controller, sid)
        case "OPTIONS" => controller.parseId(sid).map(res => idOptionsRoutingHandler())
        case _         => controller.parseId(sid).map(res => methodNotAllowed)
      }

      case _ => None
    }
  }
}


class SubRestResourceRouter[P, C <: BaseResource : ResourceWrapper](val name: String, val factory: P => C) extends AbstractRestResourceRouter[C] {
  var routeMap = Map[String, Routing[C]]()
  override val caps = super.caps ++ ResourceCaps.ValueSet(ResourceCaps.Child)
  def withParent(id: P) = new RestResourceRouter[C](factory(id), routeMap)
}
