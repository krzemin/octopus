package octopus

import shapeless.tag
import shapeless.tag.@@

import scala.language.higherKinds


sealed class ValidationResult[T](private[octopus] val value: T, val errors: List[ValidationError]) extends Serializable {

  /**
    * Eager validation composition
    */
  def alsoValidate(validator: Validator[T]): ValidationResult[T] =
    new ValidationResult(value, errors ++ validator.validate(value))

  def alsoValidateAsync[M[_]: AppError](asyncValidator: AsyncValidatorM[M, T]): M[ValidationResult[T]] =
    AppError[M].map(asyncValidator.validate(value)) { asyncErrors =>
      new ValidationResult(value, errors ++ asyncErrors)
    }

  /**
    * Short-cirtuit validation composition
    */
  def thenValidate(validator: Validator[T]): ValidationResult[T] =
    if(isValid) alsoValidate(validator) else this

  def thenValidateAsync[M[_]: AppError](asyncValidator: AsyncValidatorM[M, T]): M[ValidationResult[T]] =
    if(isValid) alsoValidateAsync(asyncValidator) else AppError[M].pure(this)

  def isValid: Boolean =
    errors.isEmpty

  def toOption: Option[T] =
    errors match {
      case Nil => Some(value)
      case _ => None
    }

  def toEither: Either[List[ValidationError], T] =
    errors match {
      case Nil => Right(value)
      case errs => Left(errs)
    }

  def toTaggedEither[Tag]: Either[List[ValidationError], T @@ Tag] =
    toEither.fold(Left(_), t => Right(tag[Tag](t)))

  def toFieldErrMapping: List[(String, String)] =
    errors.map(_.toPair)

  override def toString: String = {
    val errs = errors.map(ve => "  " + ve.asString).mkString("\n")
    val status = if(isValid) "valid" else s"invalid:\n$errs"
    s"$value\n$status\n"
  }
}
