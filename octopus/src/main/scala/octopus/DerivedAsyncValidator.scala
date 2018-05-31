package octopus

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

import scala.language.higherKinds

class DerivedAsyncValidator[F[_], T](val av: AsyncValidator[F, T]) extends AnyVal

object DerivedAsyncValidator extends LowPriorityAsyncValidatorDerivation {

  def apply[F[_], T](av: AsyncValidator[F, T]): DerivedAsyncValidator[F, T] = new DerivedAsyncValidator[F, T](av)

  implicit def hnilValidator[F[_]: App]: DerivedAsyncValidator[F, HNil] = DerivedAsyncValidator(AsyncValidator[F, HNil])

  implicit def hconsValidator[F[_], L <: Symbol, H, T <: HList]
  (
    implicit
    label: Witness.Aux[L],
    hv: Lazy[AsyncValidator[F, H]],
    tv: DerivedAsyncValidator[F, T],
    app: App[F]
  ): DerivedAsyncValidator[F, FieldType[L, H] :: T] =
    DerivedAsyncValidator {
      AsyncValidator.instance { (hlist: FieldType[L, H] :: T) =>
        app.map2(
          hv.value.validate(hlist.head),
          tv.av.validate(hlist.tail))(_ ++ _)
      }
    }

  implicit def genValidator[F[_]: App, T, Repr](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    dav: Lazy[DerivedAsyncValidator[F, Repr]]
  ): DerivedAsyncValidator[F, T] =
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
