package models

case class Color(id: Int, name: String, rgb: String) extends CachedItem

object ColorContainer extends CachedContainer[Color]{
  val cacheKey = "colors"

  val defaultItems = Map(
    1 -> Color(1, "red", "#ff0000"),
    2 -> Color(2, "green", "#00ff00"),
    3 -> Color(3, "blue", "#0000ff"),
    4 -> Color(4, "yellow", "#ffff00")
  )

  def add(name: String, rgb: String) = {
    addItem(Color(nextId, name, rgb))
  }
}
