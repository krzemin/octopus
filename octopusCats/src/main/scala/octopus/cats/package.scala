package octopus

import _root_.cats.data.{NonEmptyList, Validated, ValidatedNel}

package object cats {

  implicit class OctopusCatsOps[T](val vr: ValidationResult[T]) extends AnyVal with Serializable {

    def toValidatedNel: ValidatedNel[ValidationError, T] = vr.errors match {
      case Nil =>
        Validated.Valid(vr.value)
      case head :: tail =>
        Validated.Invalid(NonEmptyList.of(head, tail : _*))
    }

    def toValidated: Validated[List[ValidationError], T] = vr.errors match {
      case Nil =>
        Validated.Valid(vr.value)
      case errs =>
        Validated.Invalid(errs)
    }
  }
}
