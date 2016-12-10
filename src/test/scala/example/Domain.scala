package example

import octopus._

object Domain {

  case class UserId(id: Int) extends AnyVal

  object UserId {

    val Err_MustBePositive = "must be positive number"

    implicit val validator: Validator[UserId] = Validator[UserId]
      .rule(_.id > 0, Err_MustBePositive)
  }

  case class Email(address: String) extends AnyVal

  object Email {

    val Err_MustNotBeEmpty = "must not be empty"
    val Err_MustContainAt = "must contain @"
    val Err_MustContainDotAfterAt = "must contain . after @"

    implicit val validator: Validator[Email] = Validator[Email]
      .rule(_.address.nonEmpty, Err_MustNotBeEmpty)
      .rule(_.address.contains("@"), Err_MustContainAt)
      .rule(_.address.split('@').last.contains("."), Err_MustContainDotAfterAt)
  }

  case class PostalCode(code: String) extends AnyVal

  object PostalCode {

    val Err_MustBeLengthOf5 = "must be of length 5"
    val Err_MustContainOnlyDigits = "must contain only digits"

    implicit val validator: Validator[PostalCode] = Validator[PostalCode]
      .ruleVC((_: String).length == 5, Err_MustBeLengthOf5)
      .ruleVC((_: String).forall(_.isDigit), Err_MustContainOnlyDigits)
  }

  case class Address(street: String,
                     postalCode: PostalCode,
                     city: String)

  object Address {

    val Err_MustNotBeEmpty = "must not be empty"

    implicit val validator: Validator[Address] = Validator
      .derived[Address] // derives default validator for Address
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

    implicit val validator: Validator[Circle] = Validator[Circle].
      rule(_.radius > 0, Err_RadiusMustBePositive)
  }

  case class Rectangle(width: Double, height: Double) extends Shape

  object Rectangle {

    val Err_WidthMustBePositive = "width must be greater than 0"
    val Err_HeightMustBePositive = "height must be greater than 0"

    implicit val validator: Validator[Rectangle] = Validator[Rectangle]
      .rule(_.width > 0, Err_WidthMustBePositive)
      .rule(_.height > 0, Err_HeightMustBePositive)
  }
}
