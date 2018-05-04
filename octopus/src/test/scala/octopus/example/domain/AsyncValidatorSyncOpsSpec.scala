package octopus.example.domain

import octopus.Fixtures
import octopus.dsl._
import octopus.syntax._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{AsyncWordSpec, MustMatchers}

class AsyncValidatorSyncOpsSpec extends AsyncWordSpec
  with ScalaFutures
  with Fixtures
  with IntegrationPatience
  with MustMatchers {

  "AsyncValidatorSyncOps" when {

    "basic validation" should {
      implicit val v = AsyncValidator[User]
        .rule(u => u.email.address == email_Valid.address, "Invalid email")

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
        .ruleField('email, (e: Email) => e.address == email_Valid.address, "Invalid email")

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
          u => u.email.address.headOption.map(_ == 'x'),
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
  }

}