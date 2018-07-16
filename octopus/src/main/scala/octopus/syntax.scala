package octopus

import scala.language.higherKinds

object syntax {

  implicit class ValidationOps[T](val obj: T) extends AnyVal {

    def validate(implicit v: Validator[T]): ValidationResult[T] =
      new ValidationResult(obj, v.validate(obj))

    def isValid(implicit v: Validator[T]): Boolean =
      validate.isValid
  }

  implicit class AsyncValidationOps[T](val obj: T) extends AnyVal {

    def validateAsync[M[_]: AppError](implicit av: AsyncValidatorM[M, T]): M[ValidationResult[T]] =
      AppError[M].map(av.validate(obj)) { errors =>
        new ValidationResult(obj, errors)
      }

    def isValidAsync[M[_]: AppError](implicit av: AsyncValidatorM[M, T]): M[Boolean] =
      AppError[M].map(validateAsync)(_.isValid)
  }

}
