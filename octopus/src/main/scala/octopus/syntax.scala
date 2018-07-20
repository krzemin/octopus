package octopus

import scala.concurrent.Future
import scala.language.higherKinds

object syntax {

  implicit class ValidationOps[T](val obj: T) extends AnyVal {

    def validate(implicit v: Validator[T]): ValidationResult[T] =
      new ValidationResult(obj, v.validate(obj))

    def isValid(implicit v: Validator[T]): Boolean =
      validate.isValid
  }

  implicit class AsyncValidationOps[T](val obj: T) extends AnyVal {

    def validateAsync(implicit av: AsyncValidatorM[Future, T], appError: AppError[Future]): Future[ValidationResult[T]] =
      appError.map(av.validate(obj)) { errors =>
        new ValidationResult(obj, errors)
      }

    def isValidAsync(implicit av: AsyncValidatorM[Future, T], appError: AppError[Future]): Future[Boolean] =
      appError.map(validateAsync)(_.isValid)

    def validateAsyncM[M[_]: AppError](implicit av: AsyncValidatorM[M, T]): M[ValidationResult[T]] =
      AppError[M].map(av.validate(obj)) { errors =>
        new ValidationResult(obj, errors)
      }

    def isValidAsyncM[M[_]: AppError](implicit av: AsyncValidatorM[M, T]): M[Boolean] =
      AppError[M].map(validateAsyncM)(_.isValid)
  }

}
