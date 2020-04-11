package octopus

import shapeless.labelled.FieldType
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, LabelledGeneric, Lazy, Witness}

class DerivedValidator[T](val v: Validator[T]) extends AnyVal

object DerivedValidator extends LowPriorityValidatorDerivation {
  def apply[T](v: Validator[T]): DerivedValidator[T] = new DerivedValidator[T](v)
}

trait LowPriorityValidatorDerivation extends Serializable{

  implicit val hnilValidator: DerivedValidator[HNil] = DerivedValidator(Validator[HNil])

  implicit def hconsValidator[L <: Symbol, H, T <: HList](implicit label: Witness.Aux[L],
                                                          hv: Lazy[Validator[H]],
                                                          tv: DerivedValidator[T]): DerivedValidator[FieldType[L, H] :: T] = DerivedValidator {
    (hlist: FieldType[L, H] :: T) =>
      hv.value.validate(hlist.head).map(FieldLabel(label.value) :: _) ++ tv.v.validate(hlist.tail)
  }

  // $COVERAGE-OFF$
  implicit val cnilValidator: DerivedValidator[CNil] = DerivedValidator {
    (_: CNil) => null
  }
  // $COVERAGE-ON$

  implicit def coproductValidator[L <: Symbol, H, T <: Coproduct](implicit hv: Lazy[Validator[H]],
                                                                  tv: DerivedValidator[T]): DerivedValidator[FieldType[L, H] :+: T] = DerivedValidator {
    case Inl(head) => hv.value.validate(head)
    case Inr(tail) => tv.v.validate(tail)
  }

  implicit def genValidator[T, Repr](implicit gen: LabelledGeneric.Aux[T, Repr],
                                     dv: Lazy[DerivedValidator[Repr]]): DerivedValidator[T] = DerivedValidator {
    (obj: T) => dv.value.v.validate(gen.to(obj))
  }
}
