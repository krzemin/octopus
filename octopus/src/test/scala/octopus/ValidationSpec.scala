package octopus

import octopus.example.domain._
import octopus.dsl._
import octopus.syntax._
import org.scalatest.{MustMatchers, WordSpec}

class ValidationSpec
  extends WordSpec with MustMatchers with Fixtures {

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

    "given trivial validators" should {

      "always pass" in {

        implicit val validator: Validator[UserId] = Validator[UserId]

        userId_Valid.validate.isValid mustBe true
        userId_Invalid.validate.isValid mustBe true
      }

      "never pass" in {

        implicit val validator: Validator[UserId] = Validator.invalid("never pass")

        userId_Valid.validate.errors mustBe List(ValidationError("never pass"))
        userId_Invalid.validate.errors mustBe List(ValidationError("never pass"))
      }
    }

    "given validators dealing with exceptions" should {

      "validate using 'ruleCatchOnly'" in {

        implicit val validator: Validator[PositiveInputNumber] = PositiveInputNumber.validatorCatchOnly

        PositiveInputNumber("3.5").validate.isValid mustBe true

        PositiveInputNumber("-2").validate.errors mustBe List(
          ValidationError(PositiveInputNumber.Err_MustBeGreatherThan0)
        )

        PositiveInputNumber("xxx").validate.errors mustBe List(
          ValidationError("""incorrect number: For input string: "xxx"""")
        )

        the[NullPointerException] thrownBy PositiveInputNumber(null).validate
      }

      "validate using 'ruleCatchNonFatal'" in {

        implicit val validator: Validator[PositiveInputNumber] = PositiveInputNumber.validatorCatchNonFatal

        PositiveInputNumber("3.5").validate.isValid mustBe true

        PositiveInputNumber("-2").validate.errors mustBe List(
          ValidationError(PositiveInputNumber.Err_MustBeGreatherThan0)
        )

        PositiveInputNumber("xxx").validate.errors mustBe List(
          ValidationError("""incorrect number: For input string: "xxx"""")
        )

        PositiveInputNumber(null).validate.errors mustBe List(
          ValidationError("""incorrect number: null""")
        )
      }
    }

    "given validators from various standard scala types" should {

      "validate using 'ruleTry'" in {
        implicit val validator: Validator[PositiveInputNumber] = PositiveInputNumber.validatorTry

        PositiveInputNumber("3.5").validate.isValid mustBe true

        PositiveInputNumber("-2").validate.errors mustBe List(
          ValidationError(PositiveInputNumber.Err_MustBeGreatherThan0)
        )

        PositiveInputNumber("xxx").validate.errors mustBe List(
          ValidationError("""incorrect number: For input string: "xxx"""")
        )

        PositiveInputNumber(null).validate.errors mustBe List(
          ValidationError("""incorrect number: null""")
        )
      }

      "validate using 'ruleEither'" in {

        implicit val validator: Validator[PositiveInputNumber] = PositiveInputNumber.validatorEither

        PositiveInputNumber("3.5").validate.isValid mustBe true

        PositiveInputNumber("-2").validate.errors mustBe List(
          ValidationError(PositiveInputNumber.Err_MustBeGreatherThan0)
        )

        PositiveInputNumber("xxx").validate.errors mustBe List(
          ValidationError("not a float")
        )

        PositiveInputNumber(null).validate.errors mustBe List(
          ValidationError("not a float")
        )
      }

      "validate using 'ruleOption'" in {

        implicit val validator: Validator[PositiveInputNumber] = PositiveInputNumber.validatorOption

        PositiveInputNumber("3.5").validate.isValid mustBe true

        PositiveInputNumber("-2").validate.errors mustBe List(
          ValidationError(PositiveInputNumber.Err_MustBeGreatherThan0)
        )

        PositiveInputNumber("xxx").validate.errors mustBe List(
          ValidationError("""incorrect number: None""")
        )

        PositiveInputNumber(null).validate.errors mustBe List(
          ValidationError("""incorrect number: None""")
        )
      }
    }

    "given comap" should {

      "validate lifted type" in {
        case class Lifted(value: String)

        implicit val validator: Validator[Lifted] = Validator[String]
          .rule(!_.isEmpty, "empty")
          .comap[Lifted](_.value)

        Lifted("non-empty").isValid mustBe true
        Lifted("non-empty").validate.errors mustBe Nil

        Lifted("").isValid mustBe false
        Lifted("").validate.errors mustBe List(ValidationError("empty"))
      }
    }

    "support case classes over 22 parameters" should {

      "pass in success case" in {
        val goodBigCaseClass = BigCaseClass(
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid
        )

        goodBigCaseClass.isValid mustBe true
      }

      "report errors in failure case" in {
        val badBigCaseClass = BigCaseClass(
          user_Invalid1,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Valid,
          user_Invalid2
        )

        badBigCaseClass.validate.toFieldErrMapping mustBe List(
          "user1.id" -> UserId.Err_MustBePositive,
          "user1.email" -> Email.Err_MustContainAt,
          "user1.email" -> Email.Err_MustContainDotAfterAt,
          "user1.address.postalCode" -> PostalCode.Err_MustBeLengthOf5,
          "user1.address.postalCode" -> PostalCode.Err_MustContainOnlyDigits,
          "user1.address.city" -> Address.Err_MustNotBeEmpty,
          "user1.address.street" -> Address.Err_MustNotBeEmpty,
          "user25.id" -> UserId.Err_MustBePositive
        )
      }
    }
  }

}
