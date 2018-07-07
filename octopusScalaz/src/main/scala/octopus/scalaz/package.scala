package octopus

import _root_.scalaz.{NonEmptyList, Validation, ValidationNel}

package object scalaz {

  implicit class OctopusScalazOps[T](val vr: ValidationResult[T]) extends AnyVal {

    def toValidationNel: ValidationNel[ValidationError, T] = vr.errors match {
      case Nil =>
        Validation.success(vr.value)
      case head :: tail =>
        Validation.failure(NonEmptyList.nels(head, tail : _*))
    }

    def toValidation: Validation[List[ValidationError], T] = vr.errors match {
      case Nil =>
        Validation.success(vr.value)
      case errs =>
        Validation.failure(errs)
    }
  }

}
