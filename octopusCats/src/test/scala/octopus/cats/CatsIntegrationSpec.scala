package octopus.cats

import cats.data.{NonEmptyList, Validated}
import octopus.example.domain._
import octopus.syntax._
import octopus.{Fixtures, ValidationError}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CatsIntegrationSpec
  extends AnyWordSpec with Matchers with Fixtures {

  "Cats Integration" should {

    "support ValidatedNel" when {

      "Valid scenario" in {
        1.validate.toValidatedNel mustBe Validated.Valid(1)
        userId_Valid.validate.toValidatedNel mustBe Validated.Valid(userId_Valid)
        user_Valid.validate.toValidatedNel mustBe Validated.Valid(user_Valid)
      }

      "Invalid scenario" in {

        userId_Invalid.validate.toValidatedNel mustBe Validated.Invalid(
          NonEmptyList.of(ValidationError(UserId.Err_MustBePositive))
        )

        user_Invalid1.validate.toValidatedNel.leftMap(_.map(_.toPair)) mustBe Validated.Invalid(
          NonEmptyList.of(
            "id" -> UserId.Err_MustBePositive,
            "email" -> Email.Err_MustContainAt,
            "email" -> Email.Err_MustContainDotAfterAt,
            "address.postalCode" -> PostalCode.Err_MustBeLengthOf5,
            "address.postalCode" -> PostalCode.Err_MustContainOnlyDigits,
            "address.city" -> Address.Err_MustNotBeEmpty,
            "address.street" -> Address.Err_MustNotBeEmpty
          )
        )
      }
    }

    "support Validated" when {

      "Valid scenario" in {
        1.validate.toValidated mustBe Validated.Valid(1)
        userId_Valid.validate.toValidated mustBe Validated.Valid(userId_Valid)
        user_Valid.validate.toValidated mustBe Validated.Valid(user_Valid)
      }

      "Invalid scenario" in {

        userId_Invalid.validate.toValidated mustBe Validated.Invalid(
          List(ValidationError(UserId.Err_MustBePositive))
        )

        user_Invalid1.validate.toValidated.leftMap(_.map(_.toPair)) mustBe Validated.Invalid(
          List(
            "id" -> UserId.Err_MustBePositive,
            "email" -> Email.Err_MustContainAt,
            "email" -> Email.Err_MustContainDotAfterAt,
            "address.postalCode" -> PostalCode.Err_MustBeLengthOf5,
            "address.postalCode" -> PostalCode.Err_MustContainOnlyDigits,
            "address.city" -> Address.Err_MustNotBeEmpty,
            "address.street" -> Address.Err_MustNotBeEmpty
          )
        )
      }
    }
  }

}
