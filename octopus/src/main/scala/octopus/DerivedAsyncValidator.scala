package octopus

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

import scala.concurrent.ExecutionContext

class DerivedAsyncValidator[T](val av: AsyncValidator[T]) extends AnyVal

object DerivedAsyncValidator extends LowPriorityAsyncValidatorDerivation {

  def apply[T](av: AsyncValidator[T]): DerivedAsyncValidator[T] = new DerivedAsyncValidator[T](av)

  implicit val hnilValidator: DerivedAsyncValidator[HNil] = DerivedAsyncValidator(AsyncValidator[HNil])

  implicit def hconsValidator[L <: Symbol, H, T <: HList](implicit label: Witness.Aux[L],
                                                          hv: Lazy[AsyncValidator[H]],
                                                          tv: DerivedAsyncValidator[T]): DerivedAsyncValidator[FieldType[L, H] :: T] =
    DerivedAsyncValidator {
      AsyncValidator.instance { (hlist: FieldType[L, H] :: T, passedEC: ExecutionContext) =>

        implicit val ec: ExecutionContext = passedEC

        val headValidationErrorsF = hv.value
          .validate(hlist.head)
          .map(errors => errors.map(FieldLabel(label.value) :: _))

        val tailValidationErrorsF = tv.av
          .validate(hlist.tail)

        (headValidationErrorsF zip tailValidationErrorsF)
          .map { case (hErrs, tErrs) => hErrs ++ tErrs }
      }
    }

  implicit def genValidator[T, Repr](implicit gen: LabelledGeneric.Aux[T, Repr],
                                     dav: Lazy[DerivedAsyncValidator[Repr]]): DerivedAsyncValidator[T] =
    DerivedAsyncValidator {
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        dav.value.av.validate(gen.to(obj))(ec)
      }
    }

}

trait LowPriorityAsyncValidatorDerivation {

  implicit def fromSyncValidator[T](implicit v: Validator[T]): DerivedAsyncValidator[T] =
    DerivedAsyncValidator(AsyncValidator.lift(v))
}
