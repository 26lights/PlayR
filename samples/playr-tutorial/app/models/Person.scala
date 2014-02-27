package models

case class Person(id: Int, name: String) extends CachedItem

object PersonContainer extends CachedContainer[Person]{
  val cacheKey = "persons"

  val defaultItems = Map(
    1 -> Person(1, "John"),
    2 -> Person(2, "Jane"),
    3 -> Person(3, "Alice"),
    4 -> Person(4, "Bob")
  )

  def add(name: String) = {
    addItem(Person(nextId, name))
  }
}
