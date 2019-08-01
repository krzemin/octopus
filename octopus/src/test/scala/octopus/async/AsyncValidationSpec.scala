package octopus.async

import java.io.IOException

import octopus.async.ToFuture.syntax._
import octopus.dsl._
import octopus.example.domain._
import octopus.syntax._
import octopus.{AppError, FieldLabel, FieldPath, Fixtures, ValidationError}
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.language.higherKinds

abstract class AsyncValidationSpec[M[_]: ToFuture] extends AsyncWordSpec with Fixtures with MustMatchers {

  implicit def appError: AppError[M]

  private val emailServiceStub = new EmailService[M] {
    def isEmailTaken(email: String): M[Boolean] =
      AppError[M].pure(email.length <= 10)

    def doesDomainExists(email: String): M[Boolean] = {
      AppError[M].pure {
        val domain = email.dropWhile(_ != '@').tail
        Set("y.com", "example.com").contains(domain)
      }
    }
  }

  private val geoServiceStub = new GeoService[M] {
    def doesPostalCodeExist(postalCode: PostalCode.T): M[Boolean] =
      AppError[M].pure(postalCode.size == 5 && postalCode.groupBy(identity).size == 1)

    def isPostalCodeValidForCity(postalCode: PostalCode.T, city: String): M[Boolean] = {
      if(city == "Los Angeles") {
        AppError[M].pure(true)
      } else {
        throw new RuntimeException("Not implemented yet")
      }
    }
  }

  private val asyncValidators = new AsyncValidators(emailServiceStub, geoServiceStub)



  "Asynchronous validation" when {

    import asyncValidators._

    "have explicit async validator in scope" should {

      "validate using validators from scope" should {

        "case 1 - valid" in {
          email_Valid.isValidAsync.toFuture().map(_ mustBe true)
        }

        "case 2 - invalid" in {
          email_Valid_Long
            .validateAsync
            .toFuture()
            .map(_.errors must contain(ValidationError(Email_Err_AlreadyTaken)))
        }
      }

      "automatically derive AsyncValidator instances" should {
        "case 1 - valid" in {
          user_Valid.isValidAsync.toFuture().map(_ mustBe true)
        }

        "case 2 - invalid" in {
          val address_InvalidPostalCode = address_Valid.copy(postalCode = PostalCode("23456"))
          val user_InvalidPostalCode = user_Valid.copy(address = address_InvalidPostalCode)

          user_InvalidPostalCode.validateAsync.toFuture()
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

    def validateEmailF(email: Email): M[Boolean] =
      AppError[M].pure {
        email.address match {
          case e if e == email_Valid.address => true
          case _ => false
        }
      }

    implicit val userWithEmailValidator: AsyncValidatorM[M, User] =
      Validator[User]
        .asyncM[M]
        .rule[Email](_.email, validateEmailF, Exception_HandledDuringValidation)

    "don't have explicit async validator in scope" should {

      "auto generate trivial async validator instance from the sync one" should {

        "case 1 - valid all in all" in {
          email_Valid.isValidAsync.toFuture().map(_ mustBe true)
        }

        "case 2 - invalid in the context of async, but valid here" in {
          email_Valid_Long.isValidAsync.toFuture().map(_ mustBe true)
        }
      }
    }

    "have invalid validator in the scope" should {

      val Email_Err_ExternalCause = "External cause"

      implicit val invalidValidator: AsyncValidatorM[M, Email] =
        AsyncValidatorM.invalid[M, Email](Email_Err_ExternalCause)

      "be invalid on valid case" in {
        email_Valid.isValidAsync.toFuture().map(_ mustBe false)
        email_Valid.validateAsync.toFuture().map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }

      "be invalid with predefined error on invalid case" in {
        email_Invalid1.isValidAsync.toFuture().map(_ mustBe false)
        email_Invalid1.validateAsync.toFuture().map(_.errors must contain(ValidationError(Email_Err_ExternalCause)))
      }
    }
    "have broken validator in the scope" should {
      val Email_Err_ExternalCause = "External cause outside M"

      val validateF: Email => M[Boolean] = (email: Email) => email match {
        case e if e.address == email_Invalid1.address => AppError[M].failed(new IOException("nvm"))
        case _ => throw new RuntimeException(Email_Err_ExternalCause)
      }

      val emailValidator = Validator[Email]
        .asyncM[M]

      "catch non fatal error outside of targeted M" in {
        implicit val emailValidatorWithBrokenRule = emailValidator
          .ruleCatchNonFatal(validateF, "Email invalid", e => e.getMessage)

        val expectedValidationError = ValidationError(
          message = Email_Err_ExternalCause
        )

        email_Invalid2.isValidAsync.toFuture().map(_ mustBe false)
        email_Invalid2.validateAsync.toFuture().map(_.errors must contain (expectedValidationError))
      }
      "catch only handle error outside of targeted M" in {
        implicit val emailValidatorWithBrokenRule = emailValidator
          .ruleCatchOnly[IOException](validateF, "Email invalid", e => e.getMessage)

        email_Invalid2.isValidAsync.toFuture().failed.map(_ mustBe an[RuntimeException])
      }
    }

    "Validate simple email" should {

      "accept user With valid email" in {
        user_Valid.isValidAsync.toFuture().map(_ mustBe true)
      }

      "reject user With invalid email" in {
        user_Invalid3.isValidAsync.toFuture().map(_ mustBe false)
      }

      "rejected user errors should contain proper error massage" in {
        user_Invalid3.validateAsync.toFuture()
          .map(_.errors must contain(expectedValidationException))

        // this test is pretty fucked up, as this doesn't pick rules from asyncValidators object
        // note they are not imported here! FIX this up!

//        user_Invalid1.validateAsync.toFuture().map {
//          result =>
//            println(result.toFieldErrMapping)
//            result.errors must contain(expectedValidationException)
//        }
      }
    }

    "validate result with async validator" should {
      "also validate Async" in {

        implicit val userValidator: Validator[User] = Validator[User]

        user_Invalid3.validate.alsoValidateAsync(userWithEmailValidator)
          .toFuture()
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async valid regular validator case" in {
        implicit val userValidator: Validator[User] = Validator[User]

        user_Invalid3.validate.thenValidateAsync(userWithEmailValidator)
          .toFuture()
          .map(_.errors must contain(expectedValidationException))
      }

      "then validate async with invalid regular case" in {
        val expectedValidationError = List(ValidationError(
          message = "Only user with id 2 is allowed"
        ))

        implicit val userValidator: Validator[User] = Validator[User]
          .rule(_.id.id == 2, "Only user with id 2 is allowed")

        user_Invalid3.validate.thenValidateAsync(userWithEmailValidator)
          .toFuture()
          .map(_.errors must contain theSameElementsAs expectedValidationError)
      }
    }

  }
}
