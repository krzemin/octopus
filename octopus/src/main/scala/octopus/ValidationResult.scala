package octopus

import shapeless.tag
import shapeless.tag.@@

import scala.language.higherKinds


sealed class ValidationResult[T](private[octopus] val value: T, val errors: List[ValidationError]) {

  /**
    * Eager validation composition
    */
  def alsoValidate(validator: Validator[T]): ValidationResult[T] =
    new ValidationResult(value, errors ++ validator.validate(value))

  def alsoValidateAsync[M[_]: App](asyncValidator: AsyncValidator[M, T]): M[ValidationResult[T]] =
    implicitly[App[M]].map(asyncValidator.validate(value)) { asyncErrors =>
      new ValidationResult(value, errors ++ asyncErrors)
    }

  /**
    * Short-cirtuit validation composition
    */
  def thenValidate(validator: Validator[T]): ValidationResult[T] =
    if(isValid) alsoValidate(validator) else this

  def thenValidateAsync[M[_]: App](asyncValidator: AsyncValidator[M, T]): M[ValidationResult[T]] =
    if(isValid) alsoValidateAsync(asyncValidator) else implicitly[App[M]].pure(this)

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
    toEither.right.map(tag[Tag](_))

  def toFieldErrMapping: List[(String, String)] =
    errors.map(_.toPair)

  override def toString: String = {
    val errs = errors.map(ve => "  " + ve.asString).mkString("\n")
    val status = if(isValid) "valid" else s"invalid:\n$errs"
    s"$value\n$status\n"
  }
}
