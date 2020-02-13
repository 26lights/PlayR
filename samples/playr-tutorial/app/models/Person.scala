package models

import play.api.cache.SyncCacheApi
import javax.inject.Inject

case class Person(id: Int, name: String) extends CachedItem

case class PersonContainer @Inject() (cache: SyncCacheApi) extends CachedContainer[Person] {
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
