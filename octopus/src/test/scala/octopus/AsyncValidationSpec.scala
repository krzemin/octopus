package octopus

import octopus.ToFuture.syntax._
import octopus.dsl._
import octopus.example.domain._
import octopus.syntax._
import octopus.{AsyncValidatorM => _}
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.language.higherKinds

abstract class AsyncValidationSpec[M[_]: AppError : ToFuture] extends AsyncWordSpec with Fixtures with MustMatchers {

  "AsyncValidation scoping" when {

    val emailServiceStub = new EmailService[M] {
      def isEmailTaken(email: String): M[Boolean] =
        AppError[M].pure(email.length <= 10)

      def doesDomainExists(email: String): M[Boolean] = {
        AppError[M].pure {
          val domain = email.dropWhile(_ != '@').tail
          Set("y.com", "example.com").contains(domain)
        }
      }
    }

    val geoServiceStub = new GeoService[M] {
      def doesPostalCodeExist(postalCode: PostalCode.T): M[Boolean] =
        AppError[M].pure(postalCode.size == 5 && postalCode.groupBy(identity).size == 1)
    }

    val asyncValidators = new AsyncValidators(emailServiceStub, geoServiceStub)

    "have explicit async validator in scope" should {
      import asyncValidators._

      "validate using validators from scope" should {

        "case 1 - valid" in {
          email_Valid.isValidAsyncM.toFuture.map(_ mustBe true)
        }

        "case 2 - invalid" in {
          email_Valid_Long
            .validateAsyncM
            .toFuture
            .map(_.errors must contain(ValidationError(Email_Err_AlreadyTaken)))
        }
      }

      "automatically derive AsyncValidator instances" should {
        "case 1 - valid" in {
          user_Valid.isValidAsyncM.toFuture.map(_ mustBe true)
        }

        "case 2 - invalid" in {
          val address_InvalidPostalCode = address_Valid.copy(postalCode = PostalCode("23456"))
          val user_InvalidPostalCode = user_Valid.copy(address = address_InvalidPostalCode)

          user_InvalidPostalCode.validateAsyncM.toFuture
            .map {_.toFieldErrMapping mustBe List(
              "address.postalCode" -> PostalCode_Err_DoesNotExist
            )
          }
        }
      }
    }
  }

  "AsyncValidation base usage" when {

    val Exception_HandledDuringValidation = "Exception handled during validation"

    val expectedValidationException = ValidationError(
      message = Exception_HandledDuringValidation,
      path = FieldPath(List(FieldLabel('email)))
    )

    def validateEmail(email: Email): Boolean = email.address match {
      case e if e == email_Valid.address => true
      case _ => false
    }

    def validateEmailF(email: Email): M[Boolean] =
      AppError[M].pure(validateEmail(email))

    implicit val userWithEmailValidator: AsyncValidatorM[M, User] =
      octopus.Validator[User].asyncM[M].rule[Email](_.email, validateEmailF, Exception_HandledDuringValidation)

    "don't have explicit async validator in scope" should {

      "auto generate trivial async validator instance from the sync one" should {

        "case 1 - valid all in all" in {
          email_Valid.isValidAsyncM.toFuture.map(_ mustBe true)
        }

        "case 2 - invalid in the context of async, but valid here" in {
          email_Valid_Long.isValidAsyncM.toFuture.map(_ mustBe true)
        }
      }
    }

    "have invalid validator in the scope" should {
      val Email_Err_ExternalCause = "External cause"
      implicit val invalidValidator = AsyncValidatorM.invalid[M, Email](Email_Err_ExternalCause)

      "be invalid on valid case" in {
        email_Valid.isValidAsyncM.toFuture.map(_ mustBe false)
        email_Valid.validateAsyncM.toFuture.map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }

      "be invalid with predefined error on invalid case" in {
        email_Invalid1.isValidAsyncM.toFuture.map(_ mustBe false)
        email_Invalid1.validateAsyncM.toFuture.map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }
    }

    "Validate simple email" should {

      "accept user With valid email" in {
        user_Valid.isValidAsyncM.toFuture.map(_ mustBe true)
      }

      "reject user With invalid email" in {
        user_Invalid3.isValidAsyncM.toFuture.map(_ mustBe false)
      }

      "rejected user errors should contain proper error massage" in {
        user_Invalid3.validateAsyncM.toFuture.map(_.errors must contain(expectedValidationException))
      }
    }

    "validate result with async validator" should {
      "also validate Async" in {

        implicit val userValidator = octopus.Validator[User]

        user_Invalid3.validate.alsoValidateAsync(userWithEmailValidator)
          .toFuture
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async valid regular validator case" in {
        implicit val userValidator = octopus.Validator[User]

        user_Invalid3.validate.thenValidateAsync(userWithEmailValidator)
          .toFuture
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async with invalid regular case" in {
        val expectedValidationError = List(ValidationError(
          message = "Onlu user with id 2 is allowed"
        ))

        implicit val userValidator = octopus.Validator[User]
          .rule(_.id.id == 2, "Onlu user with id 2 is allowed")

        user_Invalid3.validate.thenValidateAsync(userWithEmailValidator)
          .toFuture
          .map(_.errors must contain theSameElementsAs expectedValidationError)
      }
    }

  }
}
