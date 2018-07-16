package octopus

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

import scala.language.higherKinds

class DerivedAsyncValidator[F[_], T](val av: AsyncValidatorM[F, T]) extends AnyVal

object DerivedAsyncValidator extends LowPriorityAsyncValidatorDerivation {

  def apply[M[_]: AppError, T](av: AsyncValidatorM[M, T]): DerivedAsyncValidator[M, T] = new DerivedAsyncValidator[M, T](av)

  implicit def hnilValidator[M[_]: AppError]: DerivedAsyncValidator[M, HNil] = DerivedAsyncValidator(AsyncValidatorM[M, HNil])

  implicit def hconsValidator[M[_]: AppError, L <: Symbol, H, T <: HList]
  (
    implicit
    label: Witness.Aux[L],
    hv: Lazy[AsyncValidatorM[M, H]],
    tv: DerivedAsyncValidator[M, T]
  ): DerivedAsyncValidator[M, FieldType[L, H] :: T] =
    DerivedAsyncValidator {
      AsyncValidatorM.instance { (hlist: FieldType[L, H] :: T) =>
        AppError[M].map2(
          AppError[M].map(hv.value.validate(hlist.head)){ errors =>
            errors.map(FieldLabel(label.value) :: _)
          },
          tv.av.validate(hlist.tail))(_ ++ _)
      }
    }

  implicit def genValidator[M[_]: AppError, T, Repr](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    dav: Lazy[DerivedAsyncValidator[M, Repr]]
  ): DerivedAsyncValidator[M, T] =
    DerivedAsyncValidator {
      AsyncValidatorM.instance { (obj: T) =>
        dav.value.av.validate(gen.to(obj))
      }
    }
}

trait LowPriorityAsyncValidatorDerivation {

  implicit def fromSyncValidator[F[_]: AppError, T](implicit v: Validator[T]): DerivedAsyncValidator[F, T] =
    DerivedAsyncValidator(AsyncValidatorM.lift(v))
}
