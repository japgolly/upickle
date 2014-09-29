package upickle.blah

import upickle.elsewhere._

object Eg1 {
  import upickle._
  def blah(j: String): YYY = read[YYY](j)
}
