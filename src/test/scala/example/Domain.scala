package example

import octopus._

object Domain {

  case class UserId(id: Int) extends AnyVal

  object UserId {

    implicit val validator: Validator[UserId] = Validator[UserId]
      .rule(_.id > 0, "must be positive number")
  }

  case class Email(address: String) extends AnyVal

  object Email {

    implicit val validator: Validator[Email] = Validator[Email]
      .rule(_.address.nonEmpty, "must not be empty")
      .rule(_.address.contains("@"), "must contain @")
      .rule(_.address.split('@').last.contains("."), "must contain . after @")
  }

  case class PostalCode(code: String) extends AnyVal

  object PostalCode {

    implicit val validator: Validator[PostalCode] = Validator[PostalCode]
      .ruleVC((_: String).length == 5, "must be of length 5")
      .ruleVC((_: String).forall(_.isDigit), "must contain only digits")
  }

  case class Address(street: String,
                     postalCode: PostalCode,
                     city: String)

  object Address {

    implicit val validator: Validator[Address] = Validator
      .derived[Address] // derives default validator for Address
      .ruleField('city, (_: String).nonEmpty, "must not be empty")
      .ruleField('street, (_: String).nonEmpty, "must not be empty")
  }

  case class User(id: UserId,
                  email: Email,
                  address: Address)


  sealed trait Shape

  case class Circle(radius: Double) extends Shape

  object Circle {

    implicit val validator: Validator[Circle] = Validator[Circle].
      rule(_.radius > 0, "radius must be greater than 0")
  }

  case class Rectangle(width: Double, height: Double) extends Shape

  object Rectangle {

    implicit val validator: Validator[Rectangle] = Validator[Rectangle]
      .rule(_.width > 0, "width must be greater than 0")
      .rule(_.height > 0, "height must be greater than 0")
  }
}
