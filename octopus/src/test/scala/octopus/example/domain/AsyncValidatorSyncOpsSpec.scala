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
  }

}