package octopus.scalaz

import octopus.example.domain._
import octopus.syntax._
import octopus.{Fixtures, ValidationError}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalaz.{NonEmptyList, Validation}


class ScalazIntegrationSpec
  extends AnyWordSpec with Matchers with Fixtures {

  "Scalaz Integration" should {
    "support ValidationNel" when {

      "success scenario" in {
        1.validate.toValidationNel mustBe Validation.success(1)
        userId_Valid.validate.toValidationNel mustBe Validation.success(userId_Valid)
        user_Valid.validate.toValidationNel mustBe Validation.success(user_Valid)
      }

      "failure scenario" in {

        userId_Invalid.validate.toValidationNel mustBe Validation.failure(
          NonEmptyList.nels(ValidationError(UserId.Err_MustBePositive))
        )

        user_Invalid1.validate.toValidationNel.leftMap(_.map(_.toPair)) mustBe Validation.failure(
          NonEmptyList.nels(
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

    "support Validation" when {

      "success scenario" in {
        1.validate.toValidation mustBe Validation.success(1)
        userId_Valid.validate.toValidation mustBe Validation.success(userId_Valid)
        user_Valid.validate.toValidation mustBe Validation.success(user_Valid)
      }

      "failure scenario" in {

        userId_Invalid.validate.toValidation mustBe Validation.failure(
          List(ValidationError(UserId.Err_MustBePositive))
        )

        user_Invalid1.validate.toValidation.leftMap(_.map(_.toPair)) mustBe Validation.failure(
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
