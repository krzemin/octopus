package octopus

import octopus.ExampleDomain.{PostalCode, _}
import octopus.syntax._
import org.scalatest.{MustMatchers, WordSpec}
import shapeless.tag.@@
import shapeless.test.illTyped

class ValidationSpec extends WordSpec with MustMatchers {

  import Fixtures._

  "Validation" when {

    "given primitive types" should {

      "pass them all with empty validation" in {

        1.isValid mustBe true
        "abc".isValid mustBe true
        true.isValid mustBe true
        10.0.isValid mustBe true
        10.0f.isValid mustBe true
        'x'.isValid mustBe true
        3.toByte.isValid mustBe true
        66.toShort.isValid mustBe true
        ().isValid mustBe true
      }
    }

    "given simple value classes" should {

      "validate UserId" in {

        userId_Valid.isValid mustBe true

        userId_Invalid.validate.errors mustBe List(
          ValidationError(UserId.Err_MustBePositive)
        )
      }

      "validate Email" in {

        email_Valid.isValid mustBe true

        email_Invalid1.validate.errors mustBe List(
          ValidationError(Email.Err_MustNotBeEmpty),
          ValidationError(Email.Err_MustContainAt),
          ValidationError(Email.Err_MustContainDotAfterAt)
        )

        email_Invalid2.validate.errors mustBe List(
          ValidationError(Email.Err_MustContainAt),
          ValidationError(Email.Err_MustContainDotAfterAt)
        )

        email_Invalid3.validate.errors mustBe List(
          ValidationError(Email.Err_MustContainDotAfterAt)
        )

        email_Invalid4.validate.errors mustBe List(
          ValidationError(Email.Err_MustContainAt)
        )
      }

      "validate PostalCode" in {

        postalCode_Valid.isValid mustBe true

        postalCode_Invalid1.validate.errors mustBe List(
          ValidationError(PostalCode.Err_MustBeLengthOf5),
          ValidationError(PostalCode.Err_MustContainOnlyDigits)
        )

        postalCode_Invalid2.validate.errors mustBe List(
          ValidationError(PostalCode.Err_MustBeLengthOf5)
        )
      }
    }

    "given case classes with few fields" should {

      "validate Address" in {

        address_Valid.isValid mustBe true

        address_Invalid1.validate.toFieldErrMapping mustBe List(
          "postalCode" -> PostalCode.Err_MustBeLengthOf5,
          "postalCode" -> PostalCode.Err_MustContainOnlyDigits,
          "city" -> Address.Err_MustNotBeEmpty,
          "street" -> Address.Err_MustNotBeEmpty
        )
      }

      "validate User" in {

        user_Valid.isValid mustBe true

        user_Invalid1.validate.toFieldErrMapping mustBe List(
          "id" -> UserId.Err_MustBePositive,
          "email" -> Email.Err_MustContainAt,
          "email" -> Email.Err_MustContainDotAfterAt,
          "address.postalCode" -> PostalCode.Err_MustBeLengthOf5,
          "address.postalCode" -> PostalCode.Err_MustContainOnlyDigits,
          "address.city" -> Address.Err_MustNotBeEmpty,
          "address.street" -> Address.Err_MustNotBeEmpty
        )

        user_Invalid2.validate.toFieldErrMapping mustBe List(
          "id" -> UserId.Err_MustBePositive
        )
      }
    }

    "given Option" should {

      "always pass when empty" in {

        val emptyEmail: Option[Email] = None
        emptyEmail.isValid mustBe true
      }

      "apply validation rules of inner object when not empty" in {

        val some_email_Valid: Option[Email] = Some(email_Valid)
        val some_email_Invalid4: Option[Email] = Some(email_Invalid4)

        some_email_Valid.isValid mustBe true

        some_email_Invalid4.validate.errors mustBe List(
          ValidationError(Email.Err_MustContainAt)
        )
      }
    }

    "given Traversable" should {

      val userIds_Valid = Seq(userId_Valid, userId_Valid)
      val userIds_Invalid = userIds_Valid ++ Seq(userId_Invalid, userId_Invalid, userId_Valid)

      "validate all elements of Seq" in {

        userIds_Valid.isValid mustBe true

        userIds_Invalid.validate.toFieldErrMapping mustBe List(
          "[2]" -> UserId.Err_MustBePositive,
          "[3]" -> UserId.Err_MustBePositive
        )
      }

      "validate all elements of List" in {

        userIds_Valid.toList.isValid mustBe true

        userIds_Invalid.toList.validate.toFieldErrMapping mustBe List(
          "[2]" -> UserId.Err_MustBePositive,
          "[3]" -> UserId.Err_MustBePositive
        )
      }

      "validate all elements of Array" in {

        userIds_Valid.toArray.isValid mustBe true

        userIds_Invalid.toArray.validate.toFieldErrMapping mustBe List(
          "[2]" -> UserId.Err_MustBePositive,
          "[3]" -> UserId.Err_MustBePositive
        )
      }

      "validate all elements of Set" in {

        userIds_Valid.toSet.isValid mustBe true

        Set(email_Valid, email_Invalid2, email_Invalid3).validate.toFieldErrMapping mustBe List(
          "[1]" -> Email.Err_MustContainAt,
          "[1]" -> Email.Err_MustContainDotAfterAt,
          "[2]" -> Email.Err_MustContainDotAfterAt
        )
      }
    }

    "given Map" should {

      "validate all values prefixing potential errors with key.toString" in {

        Map(20 -> userId_Valid, 30 -> userId_Valid).isValid mustBe true

        Map(30 -> email_Invalid2, 20 -> email_Valid, 40 -> email_Invalid3)
          .validate.toFieldErrMapping mustBe List(
            "[30]" -> Email.Err_MustContainAt,
            "[30]" -> Email.Err_MustContainDotAfterAt,
            "[40]" -> Email.Err_MustContainDotAfterAt
          )
      }
    }

    "given value of sealed hierarchy" should {

      "validate according to rules" in {

        shape_circle_Valid.isValid mustBe true

        shape_circle_Invalid.validate.errors mustBe List(
          ValidationError("radius must be greater than 0")
        )

        shape_rectangle_Valid.isValid mustBe true

        shape_rectangle_Invalid.validate.errors mustBe List(
          ValidationError("width must be greater than 0"),
          ValidationError("height must be greater than 0")
        )
      }
    }

    "validation rule is overriden in local context" should {

      "respect overriden rule in validator derivation" in {

        implicit val postalCodeValidator: Validator[UserId] = Validator[UserId]
          .rule(_.id % 2 == 0, "must be even")

        user_Valid.validate.toFieldErrMapping mustBe List(
          "id" -> "must be even"
        )

        user_Invalid1.validate.toFieldErrMapping mustBe List(
          "email" -> Email.Err_MustContainAt,
          "email" -> Email.Err_MustContainDotAfterAt,
          "address.postalCode" -> PostalCode.Err_MustBeLengthOf5,
          "address.postalCode" -> PostalCode.Err_MustContainOnlyDigits,
          "address.city" -> Address.Err_MustNotBeEmpty,
          "address.street" -> Address.Err_MustNotBeEmpty
        )

        user_Invalid2.isValid mustBe true
      }
    }
  }


  "Validator DSL" should {

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

  object Fixtures {

    val userId_Valid = UserId(1)
    val userId_Invalid = UserId(0)

    val email_Valid = Email("abc@example.com")
    val email_Invalid1 = Email("")
    val email_Invalid2 = Email("abc")
    val email_Invalid3 = Email("abc@xyz")
    val email_Invalid4 = Email("abc.xyz")

    val postalCode_Valid = PostalCode("00385")
    val postalCode_Invalid1 = PostalCode("abc")
    val postalCode_Invalid2 = PostalCode("003850")

    val address_Valid = Address("Love Street", postalCode_Valid, "Los Angeles")
    val address_Invalid1 = Address("", postalCode_Invalid1, "")
    val address_Invalid2 = Address("Love Street", postalCode_Valid, "")

    val user_Valid = User(userId_Valid, email_Valid, address_Valid)
    val user_Invalid1 = User(userId_Invalid, email_Invalid2, address_Invalid1)
    val user_Invalid2 = User(userId_Invalid, email_Valid, address_Valid)

    val shape_circle_Valid: Shape = Circle(10.5)
    val shape_circle_Invalid: Shape = Circle(-3.1)

    val shape_rectangle_Valid: Shape = Rectangle(2.5, 4.2)
    val shape_rectangle_Invalid: Shape = Rectangle(0, -1.3)
  }

}
