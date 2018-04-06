package octopus.example.domain

import octopus.dsl._
import shapeless.tag
import shapeless.tag._


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

  implicit val validator: Validator[PostalCode.T] = Validator[PostalCode.T]
    .rule(_.length == 5, Err_MustBeLengthOf5)
    .rule(_.forall(_.isDigit), Err_MustContainOnlyDigits)
}

case class Address(street: String,
                   postalCode: PostalCode.T,
                   city: String)

object Address {

  val Err_MustNotBeEmpty = "must not be empty"

  implicit val validator: Validator[Address] = Validator
    .derived[Address]
    .ruleField('city, (_: String).nonEmpty, Err_MustNotBeEmpty)
    .rule(_.street, (_: String).nonEmpty, Err_MustNotBeEmpty)


}

case class User(id: UserId,
                email: Email,
                address: Address)

case class BigCaseClass(user1: User,
                        user2: User,
                        user3: User,
                        user4: User,
                        user5: User,
                        user6: User,
                        user7: User,
                        user8: User,
                        user9: User,
                        user10: User,
                        user11: User,
                        user12: User,
                        user13: User,
                        user14: User,
                        user15: User,
                        user16: User,
                        user17: User,
                        user18: User,
                        user19: User,
                        user20: User,
                        user21: User,
                        user22: User,
                        user23: User,
                        user24: User,
                        user25: User)
