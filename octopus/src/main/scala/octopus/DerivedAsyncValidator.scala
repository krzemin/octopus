package octopus

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

import scala.language.higherKinds

class DerivedAsyncValidator[F[_], T](val av: AsyncValidator[F, T]) extends AnyVal

object DerivedAsyncValidator extends LowPriorityAsyncValidatorDerivation {

  def apply[M[_]: App, T](av: AsyncValidator[M, T]): DerivedAsyncValidator[M, T] = new DerivedAsyncValidator[M, T](av)

  implicit def hnilValidator[M[_]: App]: DerivedAsyncValidator[M, HNil] = DerivedAsyncValidator(AsyncValidator[M, HNil])

  implicit def hconsValidator[M[_]: App, L <: Symbol, H, T <: HList]
  (
    implicit
    label: Witness.Aux[L],
    hv: Lazy[AsyncValidator[M, H]],
    tv: DerivedAsyncValidator[M, T]
  ): DerivedAsyncValidator[M, FieldType[L, H] :: T] =
    DerivedAsyncValidator {
      AsyncValidator.instance { (hlist: FieldType[L, H] :: T) =>
        App[M].map2(
          App[M].map(hv.value.validate(hlist.head)){ errors =>
            errors.map(FieldLabel(label.value) :: _)
          },
          tv.av.validate(hlist.tail))(_ ++ _)
      }
    }

  implicit def genValidator[M[_]: App, T, Repr](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    dav: Lazy[DerivedAsyncValidator[M, Repr]]
  ): DerivedAsyncValidator[M, T] =
    DerivedAsyncValidator {
      AsyncValidator.instance { (obj: T) =>
        dav.value.av.validate(gen.to(obj))
      }
    }
}

trait LowPriorityAsyncValidatorDerivation {

  implicit def fromSyncValidator[F[_]: App, T](implicit v: Validator[T]): DerivedAsyncValidator[F, T] =
    DerivedAsyncValidator(AsyncValidator.lift(v))
}
