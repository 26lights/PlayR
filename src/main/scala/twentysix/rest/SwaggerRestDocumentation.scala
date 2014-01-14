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

case class SwaggerOperation(method: String, nickname: String, summary: String, parameters: Seq[String]= Seq(), `type`: String="string")
object SwaggerOperation {
  implicit val jsonFormat = Json.format[SwaggerOperation]
  def simple(method: String, nickname: String) = new SwaggerOperation(method, nickname, nickname)
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

  def apiList = restApi.routeResources.map {
    case (path, resource) => SwaggerResource(path, resource.getClass.getName)
  }

  def operationList(path: String, resource: Resource) = {
    var res = List[SwaggerApi]()
    var ops = resource.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple("GET", s"List ${resource.name}"))
        case ResourceCaps.Create => Some(SwaggerOperation.simple("POST", s"Create ${resource.name}"))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = new SwaggerApi(path, "Generic operations", ops) :: res

    ops = resource.caps.flatMap{ caps =>
      caps match {
        case ResourceCaps.Read   => Some(SwaggerOperation.simple("GET", s"Get ${resource.name}"))
        case ResourceCaps.Write  => Some(SwaggerOperation.simple("PUT", s"Write ${resource.name}"))
        case ResourceCaps.Update => Some(SwaggerOperation.simple("PATCH", s"Update ${resource.name}"))
        case ResourceCaps.Delete => Some(SwaggerOperation.simple("DELETE", s"Delete ${resource.name}"))
        case ResourceCaps.Action => Some(SwaggerOperation.simple(resource.name, s"${resource.name}"))
        case _ => None
      }
    }
    if(!ops.isEmpty)
      res = new SwaggerApi(path+"/:id", "Operations on identified resource", ops) :: res

    res
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

  def resourceDesc(path: String, resource: Resource) = Action {
    val res = Json.obj(
        "apiVersion" -> apiVersion,
        "swaggerVersion" -> "1.2",
        "basePath" -> restApi.prefix,
        "resourcePath" -> path,
        "apis" -> operationList(path, resource)
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
          case ApiListing(api) => restApi.routeResources.get(api).map(resourceDesc(api, _)).getOrElse(default(requestHeader))
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