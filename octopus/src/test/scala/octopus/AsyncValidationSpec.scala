package octopus

import octopus.example.domain._
import octopus.syntax._
import org.scalatest.AsyncWordSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.concurrent.Future

class AsyncValidationSpec
  extends AsyncWordSpec with Fixtures
    with ScalaFutures with IntegrationPatience {

  val uniquenessServiceStub = new UniquenessService {
    def isEmailTaken(email: String): Future[Boolean] =
      Future.successful(email.length <= 10)
    def doesDomainExists(email: String): Future[Boolean] = {
      Future.successful {
        val domain = email.dropWhile(_ != '@').tail
        Set("y.com", "example.com").contains(domain)
      }
    }
  }

  val geoServiceStub = new GeoService {
    def doesPostalCodeExist(postalCode: PostalCode.T): Future[Boolean] =
      Future.successful(postalCode.size == 5 && postalCode.groupBy(identity).size == 1)
  }

  val asyncValidators = new AsyncValidators(uniquenessServiceStub, geoServiceStub)

  "AsyncValidation" when {

    "have explicit async validator in scope" should {
      import asyncValidators._

      "validate using validators from scope" should {

        "case 1 - valid" in {
          email_Valid.isValidAsync.map(r => assert(r))
        }

        "case 2 - invalid" in {
           email_Valid_Long.validateAsync
             .map { r =>
               assert(r.errors == List(
                 ValidationError(Email_Err_AlreadyTaken)
               ))
             }
        }
      }

      "automatically derive AsyncValidator instances" should {

        "case 1 - valid" in {
          user_Valid.isValidAsync.map(r => assert(r))
        }

        "case 2 - invalid" in {
          val address_InvalidPostalCode = address_Valid.copy(postalCode = PostalCode("23456"))
          val user_InvalidPostalCode = user_Valid.copy(address = address_InvalidPostalCode)

          user_InvalidPostalCode.validateAsync
            .map { r =>
              assert(r.toFieldErrMapping == List(
                "address.postalCode" -> PostalCode_Err_DoesNotExist
              ))
            }
        }
      }
    }

    "don't have explicit async validator in scope" should {

      "auto generate trivial async validator instance from the sync one" should {

        "case 1 - valid all in all" in {
          email_Valid.isValidAsync.map(r => assert(r))
        }

        "case 2 - invalid in the context of async, but valid here" in {
          email_Valid_Long.isValidAsync.map(r => assert(r))
        }
      }
    }
  }
}
