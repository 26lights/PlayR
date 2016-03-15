package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json
import models._
import play.api.cache.CacheApi

class ColorController(implicit colorContainer: ColorContainer) extends Resource[Color]
                                                                  with ResourceRead
                                                                  with ResourceList
                                                                  with ResourceCreate
                                                                  with ResourceWrite {
  val name = "color"

  implicit val colorFormat = Json.format[Color]

  def fromId(sid: String) = toInt(sid).flatMap(id => colorContainer.get(id))

  def list = Action { Ok(Json.toJson(colorContainer.list)) }

  def read(color: Color) = Action { Ok(Json.toJson(color)) }

  def write(color: Color) = Action(parse.json) { request =>
    val newColor = request.body.as[Color].copy(id=color.id)
    colorContainer.update(newColor)
    Ok(Json.toJson(newColor))
  }

  def create = Action(parse.json){ request =>
    val newColor = request.body.as[Color]
    colorContainer.add(newColor.name, newColor.rgb)
    Created(Json.toJson(newColor))
  }
}
