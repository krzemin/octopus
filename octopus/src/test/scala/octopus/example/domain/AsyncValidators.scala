package octopus.example.domain

import octopus.dsl._

import scala.concurrent.Future

trait EmailService {

  def isEmailTaken(email: String): Future[Boolean]
  def doesDomainExists(email: String): Future[Boolean]
}

trait GeoService {

  def doesPostalCodeExist(postalCode: PostalCode.T): Future[Boolean]
}

import scala.concurrent.ExecutionContext.Implicits.global
import octopus.App._

class AsyncValidators(emailService: EmailService,
                      geoService: GeoService) {

  val Email_Err_AlreadyTaken = "email is already taken by someone else"
  val Email_Err_DomainDoesNotExists = "domain does not exists"

  implicit val emailAsyncValidator: AsyncValidator[Future, Email] =
    Validator
      .derived[Email]
      .async[Future].ruleVC(emailService.isEmailTaken, Email_Err_AlreadyTaken)
      .async.rule(_.address, emailService.doesDomainExists, Email_Err_DomainDoesNotExists)
      .rule(_.address, (_: String).nonEmpty, Email.Err_MustNotBeEmpty)
      // repeated to check dsl behavior & to ensure that it keep the validator in the asynchronous world

  val PostalCode_Err_DoesNotExist = "postal code does not exist"

  implicit val postalCodeAsyncValidator: AsyncValidator[Future, PostalCode.T] =
    AsyncValidator[Future, PostalCode.T]
      .async.rule(geoService.doesPostalCodeExist, PostalCode_Err_DoesNotExist)
}
