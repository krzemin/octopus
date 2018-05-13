package octopus.example.domain

import octopus.dsl._
import octopus.syntax._
import octopus.{Fixtures, ValidationError}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.util.{Failure, Success, Try}

class AsyncValidatorSyncOpsSpec
  extends AsyncWordSpec
  with ScalaFutures
  with Fixtures
  with MustMatchers {

  "AsyncValidatorSyncOps" when {

    "basic validation" should {
      implicit val v = AsyncValidator[User]
        .rule(_.email.address == email_Valid.address, "Invalid email")

      "accept on proper value" in {
        user_Valid.isValidAsync.map(_ mustBe true)
      }

      "reject invalid value" in {
        user_Valid2.isValidAsync.map(_ mustBe false)
      }
    }

    "validation with mapping" should {
      implicit val v = AsyncValidator[User]
        .rule[Email](_.email, _.address == email_Valid.address, "Invalid email")

      "accept on proper value" in {
        user_Valid.isValidAsync.map(_ mustBe true)
      }

      "reject invalid value" in {
        user_Valid2.isValidAsync.map(_ mustBe false)
      }
    }

    "validate field" should {
      implicit val v = AsyncValidator[User]
        .rule(_.email,  (_: Email).address == email_Valid.address, "Invalid email")

      "accept on proper value" in {
        user_Valid.isValidAsync.map(_ mustBe true)
      }

      "reject invalid value" in {
        user_Valid2.isValidAsync.map(_ mustBe false)
      }
    }

    "validate with option case" should {
      implicit val v = AsyncValidator[User]
        .ruleOption(
          _.email.address.headOption.map(_ == 'x'),
          "Email doesn't start with x",
          "Empty email"
        )

      "approve user with email starting with x" in {
        user_Valid.isValidAsync.map(_ mustBe true)
      }

      "detect email not starting with x" in {
        val expectedError = ValidationError("Email doesn't start with x")
        user_Valid2.isValidAsync.map(_ mustBe false)
        user_Valid2.validateAsync.map(_.errors must contain(expectedError))

      }

      "detect empty email" in {
        val expectedError = ValidationError("Empty email")
        user_Invalid3.isValidAsync.map(_ mustBe false)
        user_Invalid3.validateAsync.map(_.errors must contain (expectedError))
      }
    }

    "validate with Either case" should {

      def stringValueGT10(str: String): Either[String, Boolean] = {
        Try(str.toInt) match {
          case Failure(error) => Left(s"${error.getClass.getName}: ${error.getMessage}")
          case Success(intVal) => Right(intVal > 10)
        }
      }
      val invalidMessage = "String value is not > 10"
      implicit val v = AsyncValidator[Age]
        .ruleEither(a => stringValueGT10(a.value), invalidMessage)

      "detect value >10" in {
        val age = Age("15")

        age.isValidAsync.map(_ mustBe true)
      }

      "return invalid massage on invalid case" in {
        val age = Age("3")
        val expectedError = ValidationError("String value is not > 10")

        age.isValidAsync.map(_ mustBe false)
        age.validateAsync.map(_.errors must contain (expectedError))
      }

      "return invalid message on error case" in {
        val age = Age("your name")
        val expectedError = ValidationError("java.lang.NumberFormatException: For input string: \"your name\"")

        age.isValidAsync.map(_ mustBe false)
        age.validateAsync.map(_.errors must contain (expectedError))

      }
    }

    "validate catch only" should {

      def throwingStringIntValue(value: String): Boolean = {
        val intVal = value.toInt
        if(intVal == 10) throw new IllegalArgumentException("Value is not greater then 10")
        else intVal >= 5
      }

      val invalidMessage = "Value passed was not greater then 5"

      implicit val v = AsyncValidator[Age]
        .ruleCatchOnly[NumberFormatException](
          a => throwingStringIntValue(a.value),
          invalidMessage,
          _ => "Number format exception!")

      "validate proper case" in {
        val age = Age("12")

        age.isValidAsync.map(_ mustBe true)
      }

      "validate inproper case" in {
        val age = Age("4")

        age.isValidAsync.map(_ mustBe false)
        age.validateAsync.map(_.errors must contain (ValidationError(invalidMessage)))
      }

      "catch expected error" in {
        val age = Age("some name")

        age.isValidAsync.map(_ mustBe false)
        age.validateAsync.map(_.errors must contain (ValidationError("Number format exception!")))
      }

      "fail on unexpected error" in {
        val age = Age("10")

        age.isValidAsync.map(_ mustBe false)
        age.validateAsync.failed.map(_  mustBe an [IllegalArgumentException])
      }
    }
  }

}