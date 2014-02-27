package models

import play.api.cache.Cache
import play.api.Play.current

trait CachedItem {
  def id: Int
}

trait CachedContainer[T<:CachedItem] {
  val cacheKey: String
  val defaultItems: Map[Int,T]

  def items = Cache.getOrElse[Map[Int, T]](cacheKey)(defaultItems)
  def lastId = Cache.getOrElse[Int](cacheKey+"_counter")(defaultItems.size+1)

  def get(id: Int): Option[T] = items.get(id)

  def list = items.keys

  def filterList(filter: T=>Boolean) = items.values.filter(filter).map(_.id)

  def delete(item: T): T = {
    Cache.set(cacheKey, items - item.id)
    item
  }

  def nextId ={
    val next = lastId + 1
    Cache.set(cacheKey+"_counter", next)
    next
  }

  def addItem(item: T): T = {
    Cache.set(cacheKey, items + (item.id -> item))
    item
  }

  def update(item: T): T = {
    Cache.set(cacheKey, items - item.id + (item.id -> item))
    item
  }

}