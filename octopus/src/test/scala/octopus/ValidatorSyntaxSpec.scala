package octopus

import octopus.example.domain._
import octopus.dsl._
import octopus.syntax._
import org.scalatest.{MustMatchers, WordSpec}
import shapeless.tag.@@
import shapeless.test.illTyped


class ValidatorSyntaxSpec
  extends WordSpec with MustMatchers with Fixtures {

  "Validator Syntax" should {

    "validate as string summary" in {

      address_Valid.validate.toString mustBe
        s"""$address_Valid
           |valid
           |""".stripMargin

      address_Invalid1.validate.toString mustBe
        s"""$address_Invalid1
           |invalid:
           |  postalCode: ${PostalCode.Err_MustBeLengthOf5}
           |  postalCode: ${PostalCode.Err_MustContainOnlyDigits}
           |  city: ${Address.Err_MustNotBeEmpty}
           |  street: ${Address.Err_MustNotBeEmpty}
           |""".stripMargin
    }

    "validate as boolean" in {

      email_Valid.validate.isValid mustBe true

      email_Invalid4.validate.isValid mustBe false
    }

    "validate as option" in {

      email_Valid.validate.toOption mustBe Some(email_Valid)

      email_Invalid4.validate.toOption mustBe None
    }

    "validate as either" in {

      email_Valid.validate.toEither mustBe Right(email_Valid)

      email_Invalid4.validate.toEither mustBe Left(List(
        ValidationError(Email.Err_MustContainAt)
      ))
    }

    "validate as tagged either" in {

      trait Valid

      def doSomethingWithEmail(email: Email @@ Valid): Unit = ()

      email_Valid.validate.toTaggedEither[Valid].foreach(doSomethingWithEmail)

      illTyped("email_Valid.validate.toEither.foreach(doSomethingWithEmail)")
    }

    "compose validation with already validated result" should {

      val emailWithDigitValidator = Validator[Email]
        .rule(_.address.exists(_.isDigit), "must contain digit")

      "alsoValidate" should {

        "validate eagerly" in {

          email_Valid
            .validate
            .alsoValidate(emailWithDigitValidator)
            .errors mustBe List(
            ValidationError("must contain digit")
          )

          email_Invalid3
            .validate
            .alsoValidate(emailWithDigitValidator)
            .errors.map(_.message) must contain("must contain digit")
        }
      }

      "thenValidate" should {

        "validate in short-circuit manner" in {

          email_Valid
            .validate
            .thenValidate(emailWithDigitValidator)
            .errors mustBe List(
            ValidationError("must contain digit")
          )

          email_Invalid3
            .validate
            .thenValidate(emailWithDigitValidator)
            .errors.map(_.message) must not contain "must contain digit"
        }
      }
    }
  }
}
