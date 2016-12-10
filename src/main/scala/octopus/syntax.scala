package octopus

import shapeless.tag
import shapeless.tag.@@

object syntax {

  implicit class ValidationOps[T](val obj: T) extends AnyVal {

    def validate(implicit v: Validator[T]): List[ValidationError] =
      v.validate(obj)

    def validateAsEither(implicit v: Validator[T]): Either[List[ValidationError], T] =
      v.validate(obj) match {
        case Nil => Right(obj)
        case errs => Left(errs)
      }

    def validateAsTaggedEither[Tag](implicit v: Validator[T]): Either[List[ValidationError], T @@ Tag] =
      obj.validateAsEither.map(tag[Tag](_))

    def validateAsFieldErrMapping(implicit v: Validator[T]): List[(String, String)] =
      v.validate(obj).map(_.toPair)

    def isValid(implicit v: Validator[T]): Boolean =
      v.validate(obj).isEmpty
  }
}
