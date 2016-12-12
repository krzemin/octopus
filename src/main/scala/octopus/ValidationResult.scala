package octopus

import shapeless.tag
import shapeless.tag.@@


sealed class ValidationResult[T](private val value: T, val errors: List[ValidationError]) {

  def thenValidate(validator: Validator[T]): ValidationResult[T] =
    new ValidationResult(value, errors ++ validator.validate(value))

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
    toEither.map(tag[Tag](_))

  def toFieldErrMapping: List[(String, String)] =
    errors.map(_.toPair)

  override def toString: String = {
    val errs = errors.map(ve => "  " + ve.asString).mkString("\n")
    val status = if(isValid) "valid" else s"invalid:\n$errs"
    s"$value\n$status\n"
  }
}
