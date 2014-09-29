package upickle.elsewhere

sealed trait YYY
case object YY1 extends YYY

object Eg2 {
  import upickle._
  def blah(j: String): YYY = read[YYY](j)
}
