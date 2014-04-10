package controllers

import twentysix.playr._
import twentysix.playr.simple._
import play.api.mvc._
import play.api.libs.json.Json

import models._

object ColorController extends Resource[Color]
                          with ResourceRead
                          with ResourceCreate
                          with ResourceWrite {
  val name = "color"

  implicit val colorFormat = Json.format[Color]

  def fromId(sid: String) = toInt(sid).flatMap(id => ColorContainer.get(id))

  def list = Action { Ok(Json.toJson(ColorContainer.list)) }

  def read(color: Color) = Action { Ok(Json.toJson(color)) }

  def write(color: Color) = Action(parse.json) { request =>
    val newColor = request.body.as[Color].copy(id=color.id)
    ColorContainer.update(newColor)
    Ok(Json.toJson(newColor))
  }

  def create = Action(parse.json){ request =>
    val newColor = request.body.as[Color]
    ColorContainer.add(newColor.name, newColor.rgb)
    Created(Json.toJson(newColor))
  }
}
