package twentysix.rest

import play.core.Router
import scala.runtime.AbstractPartialFunction
import play.api.mvc._
import play.api.libs.json._
import twentysix.core.api.mvc.JsonAction
import play.api.Logger
import scala.reflect.runtime.universe._

case class SwaggerResource(path: String, description: String)
object SwaggerResource {
  implicit val jsonFormat = Json.format[SwaggerResource]
}

case class SwaggerParameter(name: String, description: String, paramType: String="path", `type`: String="string", required: Boolean=true)
object SwaggerParameter {
  implicit val jsonFormat = Json.format[SwaggerParameter]
}

case class SwaggerOperation(method: String, nickname: String, summary: String, parameters: Seq[SwaggerParameter], dataType: String="string")
object SwaggerOperation {
  implicit val jsonFormat = Json.format[SwaggerOperation]
  def simple(method: String, nickname: String, parameters: Seq[SwaggerParameter]) = new SwaggerOperation(method, nickname, nickname, parameters)
}

case class SwaggerApi(path: String, description: String, operations: Traversable[SwaggerOperation])
object SwaggerApi {
  implicit val jsonFormat = Json.format[SwaggerApi]
}

class SwaggerRestDocumentation(val restApi: RestRouter, val apiVersion: String="1.0") extends Router.Routes {
  protected var _prefix: String =""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private val SubPathExpression = "^(/([^/]+)).*$".r

  val apiMap = restApi.routeResources("").map{ info =>
    info.path -> info
  }.toMap

  def apiList = apiMap.map { mapping =>
    SwaggerResource(mapping._1, mapping._2.resource.getClass.getName)
  }

  def operationList(path: String, routeInfo: RestRouteInfo, parameters: Seq[SwaggerParameter] = Seq()): List[SwaggerApi] = {
    var res = List[SwaggerApi]()
    val resource = routeInfo.resource
    var ops = resource.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple("GET", s"List ${resource.name}", parameters))
        case ResourceCaps.Create => Some(SwaggerOperation.simple("POST", s"Create ${resource.name}", parameters))
        case ResourceCaps.Action => Some(SwaggerOperation.simple(resource.asInstanceOf[ResourceAction].method, resource.name, parameters))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = res :+ new SwaggerApi(path, "Generic operations", ops)

    val subParams = parameters :+ SwaggerParameter(s"${resource.name}_id", s"identified ${resource.name}")
    ops = resource.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple("GET", s"Read ${resource.name}", subParams))
        case ResourceCaps.Write  => Some(SwaggerOperation.simple("PUT", s"Write ${resource.name}", subParams))
        case ResourceCaps.Update => Some(SwaggerOperation.simple("PATCH", s"Update ${resource.name}", subParams))
        case ResourceCaps.Delete => Some(SwaggerOperation.simple("DELETE", s"Delete ${resource.name}", subParams))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = res :+ new SwaggerApi(s"$path/{${resource.name}_id}", "Operations on identified resource", ops)

    res ++ routeInfo.subResources.flatMap(info => operationList(s"$path/{${resource.name}_id}/${info.path}", info, subParams))
  }

  def resourceListing = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "apis" -> apiList
    )
    Results.Ok(Json.toJson(res))
  }

  def renderSwaggerUi = Action {
    Results.Ok(views.html.swagger(this.prefix+".json"))
  }

  def resourceDesc(path: String, routeInfo: RestRouteInfo) = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "basePath" -> restApi.prefix,
        "resourcePath" -> path,
        "apis" -> operationList(path, routeInfo)
    )
    Results.Ok(Json.toJson(res))
  }

  private val ApiListing = "^\\.json(/.*)$".r

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        path match {
          case ".json"         => resourceListing
          case ""|"/"          => renderSwaggerUi
          case ApiListing(api) => apiMap.get(api).map(resourceDesc(api, _)).getOrElse(default(requestHeader))
          case _               => default(requestHeader)
        }
      } else {
        default(requestHeader)
      }
    }

    def isDefinedAt(requestHeader: RequestHeader): Boolean = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())

        path match {
          case ""|"/" => true
          case _      => false
        }
      } else {
        false
      }
    }
  }
}