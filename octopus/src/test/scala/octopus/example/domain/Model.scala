package octopus.example.domain


import octopus.Validator
import octopus.dsl._
import shapeless.tag
import shapeless.tag._
import scala.util.Try


case class UserId(id: Int) extends AnyVal

object UserId {

  val Err_MustBePositive = "must be positive number"

  implicit val validator = Validator[UserId]
    .rule(_.id > 0, Err_MustBePositive)
}

case class Email(address: String) extends AnyVal

object Email {
  val Err_MustNotBeEmpty = "must not be empty"
  val Err_MustContainAt = "must contain @"
  val Err_MustContainDotAfterAt = "must contain . after @"

  implicit val validator = Validator[Email]
    .ruleVC[String](_.nonEmpty, Err_MustNotBeEmpty)
    .ruleVC[String](_.contains("@"), Err_MustContainAt)
    .ruleVC[String](_.split('@').last.contains("."), Err_MustContainDotAfterAt)
}


sealed trait PostalCodeTag

object PostalCode {
  type T = String @@ PostalCodeTag
  def apply(email: String): T = tag[PostalCodeTag][String](email)

  val Err_MustBeLengthOf5 = "must be of length 5"
  val Err_MustContainOnlyDigits = "must contain only digits"

}

object PostalCodeTag {
  import PostalCode._
  implicit val validator = Validator[PostalCode.T]
    .rule(_.length == 5, Err_MustBeLengthOf5)
    .rule(_.forall(_.isDigit), Err_MustContainOnlyDigits)
}

case class Address(street: String,
                   postalCode: PostalCode.T,
                   city: String)

object Address {

  val Err_MustNotBeEmpty = "must not be empty"

  implicit val validator = Validator
    .derived[Address]
    .ruleField('city, (_: String).nonEmpty, Err_MustNotBeEmpty)
    .ruleField('street, (_: String).nonEmpty, Err_MustNotBeEmpty)
}

case class User(id: UserId,
                email: Email,
                address: Address)


sealed trait Shape

case class Circle(radius: Double) extends Shape

object Circle {

  val Err_RadiusMustBePositive = "radius must be greater than 0"

  implicit val validator = Validator[Circle]
    .rule(_.radius > 0, Err_RadiusMustBePositive)
}

case class Rectangle(width: Double, height: Double) extends Shape

object Rectangle {

  val Err_WidthMustBePositive = "width must be greater than 0"
  val Err_HeightMustBePositive = "height must be greater than 0"

  implicit val validator = Validator[Rectangle]
    .rule(_.width > 0, Err_WidthMustBePositive)
    .rule(_.height > 0, Err_HeightMustBePositive)
}

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