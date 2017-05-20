package octopus

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

import scala.concurrent.ExecutionContext

trait AsyncValidatorDerivation extends LowPriorityAsyncValidatorDerivation {

  implicit val hnilValidator: AsyncValidator[HNil] = AsyncValidator[HNil]

  implicit def hconsValidator[L <: Symbol, H, T <: HList](implicit label: Witness.Aux[L],
                                                          hv: Lazy[AsyncValidator[H]],
                                                          tv: AsyncValidator[T]): AsyncValidator[FieldType[L, H] :: T] =
    AsyncValidator.instance { (hlist: FieldType[L, H] :: T, passedEC: ExecutionContext) =>

      implicit val ec: ExecutionContext = passedEC

      val headValidationErrorsF = hv.value
        .validate(hlist.head)
        .map(errors => errors.map(FieldLabel(label.value) :: _))

      val tailValidationErrorsF = tv
        .validate(hlist.tail)

      (headValidationErrorsF zip tailValidationErrorsF)
        .map { case (hErrs, tErrs) => hErrs ++ tErrs }
    }

  implicit def genValidator[T, Repr](implicit gen: LabelledGeneric.Aux[T, Repr],
                                     av: Lazy[AsyncValidator[Repr]]): AsyncValidator[T] =
    AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
      av.value.validate(gen.to(obj))(ec)
    }

}

trait LowPriorityAsyncValidatorDerivation {

  implicit def fromSyncValidator[T](implicit v: Validator[T]): AsyncValidator[T] =
    AsyncValidator.lift(v)
}
