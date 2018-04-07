package octopus

import java.io.IOException

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
  private def emailCheckThrowing(email: Email): Future[Boolean] = Future.failed(new IOException())
  private def userThrowNonFatal(user: User): Future[Boolean] = Future.failed(new Exception(Exception_handled_during_validation))

  private val Email_does_not_contain_a = "Email does not contain a"
  private val Invalid_user = "Invalid user"
  private val Exception_handled_during_validation = "Exception handled during validation"

  private val userValidator = Validator[User].async

  "AsyncValidationRules" when {

    "Simple email validator" should {

      implicit val userUniqueEmailValidator = userValidator
        .ruleField('email, isEmailUnique, Email_does_not_contain_a)

      "accept proper email" in {
        user_Valid2.isValidAsync.map(r => assert(r))
      }

      "reject invalid email" in {

        val expectedValidationError = ValidationError(
          message = Email_does_not_contain_a,
          path = FieldPath(List(FieldLabel('email)))
        )

        user_Valid.isValidAsync.map(r => r must be(false))
        user_Valid.validateAsync.map { r =>
          r.errors must contain(expectedValidationError)
        }
      }
    }

    "Throwing email validator" should {

      implicit val userUniqueThrowingValidator = userValidator
        .ruleField('email, emailCheckThrowing, Email_does_not_contain_a)

      "throw on validation check" in {
        user_Valid.isValidAsync.failed.map(r => r mustBe an [IOException])
      }
    }
    "Catch non fatal rule" should {
      implicit val userCatchNonFatal = userValidator
        .ruleCatchNonFatal(userThrowNonFatal, Invalid_user, e => e.getMessage)

      "fail validation with exception in errors" in {

        val expectedValidationException = ValidationError(
          message = Exception_handled_during_validation
        )

        user_Valid.isValidAsync.map(r => r must be (false))
        user_Valid.validateAsync.map { r =>
          r.errors must contain (expectedValidationException)
        }
      }
    }
  }
}
