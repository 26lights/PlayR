package twentysix.rest

import play.core.Router
import scala.runtime.AbstractPartialFunction
import play.api.mvc._
import play.api.libs.json._
import twentysix.core.api.mvc.JsonAction
import play.api.Logger

case class SwaggerApi(path: String, description: String)
object SwaggerApi {
  implicit val jsonFormat = Json.format[SwaggerApi]
}

class SwaggerRestDocumentation(val restApi: RestRouter) extends Router.Routes {
  protected var _prefix: String =""

  def setPrefix(newPrefix: String) = {
    _prefix = newPrefix
  }

  def prefix = _prefix
  def documentation = Nil

  private val SubPathExpression = "^(/([^/]+)).*$".r

  def apiList = restApi.routeResources.map {
    case (path, resource) => SwaggerApi(path, resource.name)
  }
  
  def resourceListing = Action {
    val res = Json.obj(
        "apiVersion" -> "0.2",
        "swaggerVersion" -> "1.2",
        "apis" -> apiList
    )
    Results.Ok(Json.toJson(res))
  }

  def renderSwaggerUi = Action {
    Results.Ok(views.html.swagger(this.prefix+".json"))
  }

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B>: Handler]( requestHeader: A, default: A => B) = {
      if(requestHeader.path.startsWith(_prefix)) {
        val path = requestHeader.path.drop(_prefix.length())
        Logger.debug(s"path=$path")
        path match {
          case ".json" => resourceListing
          case ""|"/"  => renderSwaggerUi
          case _       => default(requestHeader) 
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
          case _ => false
        }
      } else {
        false
      }
    }
  }
}