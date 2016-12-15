package octopus

object syntax {

  implicit class ValidationOps[T](val obj: T) extends AnyVal {

    def validate(implicit v: Validator[T]): ValidationResult[T] =
      new ValidationResult(obj, v.validate(obj))

    def isValid(implicit v: Validator[T]): Boolean =
      validate.isValid
  }
}
