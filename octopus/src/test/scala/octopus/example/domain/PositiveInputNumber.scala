package octopus.example.domain

import octopus.dsl._

import scala.util.Try

case class PositiveInputNumber(numberStr: String)

object PositiveInputNumber {

  val Err_MustBeGreatherThan0 = "must be greater than 0"
  def Err_IncorrectNumber(reason: String): String = s"incorrect number: $reason"
  def Err_IncorrectNumber(reason: Throwable): String = Err_IncorrectNumber(reason.getMessage)

  def isFloat(s: String): Boolean = s != null && Try(s.toFloat).isSuccess

  def parseFloat(s: String): Float = {
    if(s == null) throw new NullPointerException
    s.toFloat
  }

  val validatorCatchOnly = Validator[PositiveInputNumber]
    .ruleCatchOnly[NumberFormatException](s => parseFloat(s.numberStr) > 0, Err_MustBeGreatherThan0, Err_IncorrectNumber)

  val validatorCatchNonFatal = Validator[PositiveInputNumber]
    .ruleCatchNonFatal(s => parseFloat(s.numberStr) > 0, Err_MustBeGreatherThan0, Err_IncorrectNumber)

  val validatorTry = Validator[PositiveInputNumber]
    .ruleTry(s => Try(parseFloat(s.numberStr)).map(_ > 0), Err_MustBeGreatherThan0, Err_IncorrectNumber)

  val validatorEither = Validator[PositiveInputNumber]
    .ruleEither(n => Either.cond(isFloat(n.numberStr), n.numberStr.toFloat > 0, "not a float"), Err_MustBeGreatherThan0)

  val validatorOption = Validator[PositiveInputNumber]
    .ruleOption(n => Try(n.numberStr.toFloat).toOption.map(_ > 0), Err_MustBeGreatherThan0, Err_IncorrectNumber("None"))
}
