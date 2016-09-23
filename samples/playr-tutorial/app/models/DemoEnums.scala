package models

import twentysix.playr.utils.FilteredEnum

object MonthEnum extends Enumeration {
  val January, February, March, April, May, June, July, August, September, October, November, December = Value
}

object NameEnum extends Enumeration with FilteredEnum {
  val Mozart, Bach, Beethoven, Lully, Vivaldi = Value

  def inUse = ValueSet(Mozart, Bach, Beethoven)
}
