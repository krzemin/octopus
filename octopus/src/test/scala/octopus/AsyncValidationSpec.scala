package octopus

import octopus.dsl._
import octopus.example.domain._
import octopus.syntax._
import octopus.{AsyncValidator => _}
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.concurrent.Future

abstract class AsyncValidationSpec[M[_]] extends AsyncWordSpec with Fixtures with MustMatchers {

  def extractValueFrom[A](mval: M[A]): Future[A]

  implicit def app: App[M]


  "AsyncValidation scoping" when {

    val emailServiceStub = new EmailService[M] {
      def isEmailTaken(email: String): M[Boolean] =
        app.pure(email.length <= 10)

      def doesDomainExists(email: String): M[Boolean] = {
        app.pure {
          val domain = email.dropWhile(_ != '@').tail
          Set("y.com", "example.com").contains(domain)
        }
      }
    }

    val geoServiceStub = new GeoService[M] {
      def doesPostalCodeExist(postalCode: PostalCode.T): M[Boolean] =
        app.pure(postalCode.size == 5 && postalCode.groupBy(identity).size == 1)
    }

    val asyncValidators = new AsyncValidators(emailServiceStub, geoServiceStub)

    "have explicit async validator in scope" should {
      import asyncValidators._

      "validate using validators from scope" should {

        "case 1 - valid" in {
          extractValueFrom(email_Valid.isValidAsync).map(_ mustBe true)
        }

        "case 2 - invalid" in {
          extractValueFrom(email_Valid_Long.validateAsync)
            .map(_.errors must contain(ValidationError(Email_Err_AlreadyTaken)))
        }
      }

      "automatically derive AsyncValidator instances" should {
        "case 1 - valid" in {
          extractValueFrom(user_Valid.isValidAsync).map(_ mustBe true)
        }

        "case 2 - invalid" in {
          val address_InvalidPostalCode = address_Valid.copy(postalCode = PostalCode("23456"))
          val user_InvalidPostalCode = user_Valid.copy(address = address_InvalidPostalCode)

          extractValueFrom(user_InvalidPostalCode.validateAsync).map {
            _.toFieldErrMapping mustBe List(
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
      App[M].pure(validateEmail(email))

    implicit val userWithEmailValidator: AsyncValidator[M, User] =
      octopus.Validator[User].asyncF[M].rule[Email](_.email, validateEmailF, Exception_HandledDuringValidation)

    "don't have explicit async validator in scope" should {

      "auto generate trivial async validator instance from the sync one" should {

        "case 1 - valid all in all" in {
          extractValueFrom(email_Valid.isValidAsync).map(_ mustBe true)
        }

        "case 2 - invalid in the context of async, but valid here" in {
          extractValueFrom(email_Valid_Long.isValidAsync).map(_ mustBe true)
        }
      }
    }

    "have invalid validator in the scope" should {
      val Email_Err_ExternalCause = "External cause"
      implicit val invalidValidator = AsyncValidator.invalid[M, Email](Email_Err_ExternalCause)

      "be invalid on valid case" in {
        extractValueFrom(email_Valid.isValidAsync).map(_ mustBe false)
        extractValueFrom(email_Valid.validateAsync).map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }

      "be invalid with predefined error on invalid case" in {
        extractValueFrom(email_Invalid1.isValidAsync).map(_ mustBe false)
        extractValueFrom(email_Invalid1.validateAsync).map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }
    }

    "Validate simple email" should {

      "accept user With valid email" in {
        extractValueFrom(user_Valid.isValidAsync).map(_ mustBe true)
      }

      "reject user With invalid email" in {
        extractValueFrom(user_Invalid3.isValidAsync).map(_ mustBe false)
      }

      "rejected user errors should contain proper error massage" in {
        extractValueFrom(user_Invalid3.validateAsync).map(_.errors must contain(expectedValidationException))
      }
    }

    "validate result with async validator" should {
      "also validate Async" in {

        implicit val userValidator = octopus.Validator[User]

        extractValueFrom(user_Invalid3.validate.alsoValidateAsync(userWithEmailValidator))
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async valid regular validator case" in {
        implicit val userValidator = octopus.Validator[User]

        extractValueFrom(user_Invalid3.validate.thenValidateAsync(userWithEmailValidator))
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async with invalid regular case" in {
        val expectedValidationError = List(ValidationError(
          message = "Onlu user with id 2 is allowed"
        ))

        implicit val userValidator = octopus.Validator[User]
          .rule(_.id.id == 2, "Onlu user with id 2 is allowed")

        extractValueFrom(user_Invalid3.validate.thenValidateAsync(userWithEmailValidator))
          .map(_.errors must contain theSameElementsAs expectedValidationError)
      }
    }

  }
}
