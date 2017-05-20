package octopus.example.domain

import octopus.dsl._
import octopus.dsl.async._

import scala.concurrent.Future

trait UniquenessService {

  def isEmailTaken(email: String): Future[Boolean]
}

trait GeoService {

  def doesPostalCodeExist(postalCode: PostalCode.T): Future[Boolean]
}

class AsyncValidators(uniquenessService: UniquenessService,
                      geoService: GeoService) {

  val Email_Err_AlreadyTaken = "given email is already taken by someone else"

  implicit val emailAsyncValidator: AsyncValidator[Email] =
    Validator
      .derived[Email]
      .toAsync
      .async.ruleVC(uniquenessService.isEmailTaken, Email_Err_AlreadyTaken)


  val PostalCode_Err_DoesNotExist = "postal code does not exist"

  implicit val postalCodeAsyncValidator: AsyncValidator[PostalCode.T] =
    AsyncValidator[PostalCode.T]
      .async.rule(geoService.doesPostalCodeExist, PostalCode_Err_DoesNotExist)
}
