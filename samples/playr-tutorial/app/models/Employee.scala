package models

import play.api.cache.CacheApi

case class Employee(companyId: Int, personId: Int, function: String) extends CachedItem {
  val id = companyId * 10000 + personId
}

case class EmployeeContainer(cache: CacheApi) extends CachedContainer[Employee]{
  val cacheKey = "employee"

  val defaultItems = Map(
    10001 -> Employee(1, 1, "taster"),
    10002 -> Employee(1, 2, "ceo"),
    20004 -> Employee(2, 4, "clown")
  )

  def add(company: Company, person: Person, function: String) = {
    addItem(Employee(company.id, person.id, function))
  }
}
