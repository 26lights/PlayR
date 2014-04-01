package twentysix.playr

object ResourceCaps extends Enumeration {
  type ResourceCaps = Value
  val Read, Write, Create, Delete, Update, Parent, Child, Action = Value
}

