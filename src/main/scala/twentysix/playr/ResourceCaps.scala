package twentysix.playr

object ResourceCaps extends Enumeration {
  type ResourceCaps = Value
  val Read, Write, List, Create, Delete, Update, Parent, Child, Action, Filtered, Api = Value
}
