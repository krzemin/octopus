package octopus.example.domain

import octopus.dsl._

import octopus.App

trait EmailService[M[_]] {
  def isEmailTaken(email: String): M[Boolean]
  def doesDomainExists(email: String): M[Boolean]
}

trait GeoService[M[_]] {
  def doesPostalCodeExist(postalCode: PostalCode.T): M[Boolean]
}

class AsyncValidators[M[_]: App](emailService: EmailService[M],
                      geoService: GeoService[M]) {

  val Email_Err_AlreadyTaken = "email is already taken by someone else"
  val Email_Err_DomainDoesNotExists = "domain does not exists"

  implicit val emailAsyncValidator: AsyncValidator[M, Email] =
    Validator
      .derived[Email]
      .asyncF[M].ruleVC(emailService.isEmailTaken, Email_Err_AlreadyTaken)
      .async.rule(_.address, emailService.doesDomainExists, Email_Err_DomainDoesNotExists)
      .rule(_.address, (_: String).nonEmpty, Email.Err_MustNotBeEmpty)
      // repeated to check dsl behavior & to ensure that it keep the validator in the asynchronous world

  val PostalCode_Err_DoesNotExist = "postal code does not exist"

  implicit val postalCodeAsyncValidator: AsyncValidator[M, PostalCode.T] =
    AsyncValidator[M, PostalCode.T]
      .async.rule(geoService.doesPostalCodeExist, PostalCode_Err_DoesNotExist)
}
