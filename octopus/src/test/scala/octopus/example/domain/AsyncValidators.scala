package octopus.example.domain

import octopus.dsl._
import octopus.{AppError, AsyncValidatorM}

import scala.language.higherKinds

trait EmailService[M[_]] {
  def isEmailTaken(email: String): M[Boolean]
  def doesDomainExists(email: String): M[Boolean]
}

trait GeoService[M[_]] {
  def doesPostalCodeExist(postalCode: PostalCode.T): M[Boolean]
  def isPostalCodeValidForCity(postalCode: PostalCode.T, city: String): M[Boolean]
}

class AsyncValidators[M[_]: AppError](emailService: EmailService[M],
                                      geoService: GeoService[M]) {

  val Email_Err_AlreadyTaken = "email is already taken by someone else"
  val Email_Err_DomainDoesNotExists = "domain does not exists"

  implicit val emailAsyncValidator: AsyncValidatorM[M, Email] =
    Validator
      .derived[Email]
      .asyncM[M].ruleVC(emailService.isEmailTaken, Email_Err_AlreadyTaken)
      .async.rule(_.address, emailService.doesDomainExists, Email_Err_DomainDoesNotExists)
      .rule(_.address, (_: String).nonEmpty, Email.Err_MustNotBeEmpty)
      // repeated to check dsl behavior & to ensure that it keep the validator in the asynchronous world

  val PostalCode_Err_DoesNotExist = "postal code does not exist"

  implicit val postalCodeAsyncValidator: AsyncValidatorM[M, PostalCode.T] =
    AsyncValidatorM[M, PostalCode.T]
      .async.rule(geoService.doesPostalCodeExist, PostalCode_Err_DoesNotExist)

  val PostalCode_Err_NotValidForCity = "postal code is not valid for the city"

  implicit val addressValidator: AsyncValidatorM[M, Address] = Validator[Address]
    .asyncM[M].ruleCatchNonFatal(addr => geoService.isPostalCodeValidForCity(addr.postalCode, addr.city),
                                 PostalCode_Err_NotValidForCity,
                                 _.getMessage)
    .async.composeDerived
}
