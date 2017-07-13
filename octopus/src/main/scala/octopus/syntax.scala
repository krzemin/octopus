package octopus

import scala.concurrent.{ExecutionContext, Future}

object syntax {

  implicit class OctopusOps[T](val obj: T) extends AnyVal {

    def validate(implicit v: Validator[T]): ValidationResult[T] =
      new ValidationResult(obj, v.validate(obj))

    def isValid(implicit v: Validator[T]): Boolean =
      validate.isValid

    def normalize(implicit n: Normalizer[T]): T =
      n.normalize(obj)

    def normalizeAndValidate(implicit n: Normalizer[T], v: Validator[T]): ValidationResult[T] = {
      val normalizedObj = n.normalize(obj)
      new ValidationResult(normalizedObj, v.validate(normalizedObj))
    }
  }

  implicit class AsyncOctopusOps[T](val obj: T) extends AnyVal {

    def validateAsync(implicit av: AsyncValidator[T],
                      ec: ExecutionContext): Future[ValidationResult[T]] =
      av.validate(obj)(ec).map { errors =>
        new ValidationResult(obj, errors)
      }

    def isValidAsync(implicit av: AsyncValidator[T],
                     ec: ExecutionContext): Future[Boolean] =
      validateAsync.map(_.isValid)
  }

}
