package twentysix.playr

import play.api.mvc.RequestHeader

object RestRouteActionType extends Enumeration {
  type RestRouteActionType = Value
  val List, Read, Write, Create, Delete, Update, Traverse, Custom = Value
}
