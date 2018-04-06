package octopus

import octopus.example.domain.{Email, User}
import org.scalatest.{AsyncWordSpec, MustMatchers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import octopus.dsl._
import octopus.syntax._

import scala.concurrent.Future

class AsyncValidationRulesSpec extends AsyncWordSpec
  with ScalaFutures
  with IntegrationPatience
  with Fixtures
  with MustMatchers {

  private def isEmailUnique(email: Email): Future[Boolean] = Future.successful(email.address.contains("a"))
  private def Email_does_not_contain_a = "Email does not contain a"

  implicit val userUniqueEmailValidator = Validator[User]
    .async
    .ruleField('email, isEmailUnique, Email_does_not_contain_a)

  "AsyncValidationRules" when {

    "accept proper email" in {
      user_Valid2.isValidAsync.map(r => assert(r))
    }

    "reject invalid email" in {
      user_Valid.isValidAsync.map(r => r must be (false))
    }

  }
}
