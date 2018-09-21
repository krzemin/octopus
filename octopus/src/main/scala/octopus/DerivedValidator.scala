package octopus

import shapeless.labelled.FieldType
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, LabelledGeneric, Lazy, Witness}

class DerivedValidator[T](val v: Validator[T]) extends AnyVal

object DerivedValidator extends LowPriorityValidatorDerivation {

  def apply[T](v: Validator[T]): DerivedValidator[T] = new DerivedValidator[T](v)

  implicit val stringValidator: DerivedValidator[String] = DerivedValidator(Validator[String])
  implicit val intValidator: DerivedValidator[Int] = DerivedValidator(Validator[Int])
  implicit val boolValidator: DerivedValidator[Boolean] = DerivedValidator(Validator[Boolean])
  implicit val doubleValidator: DerivedValidator[Double] = DerivedValidator(Validator[Double])
  implicit val floatValidator: DerivedValidator[Float] = DerivedValidator(Validator[Float])
  implicit val charValidator: DerivedValidator[Char] = DerivedValidator(Validator[Char])
  implicit val byteValidator: DerivedValidator[Byte] = DerivedValidator(Validator[Byte])
  implicit val shortValidator: DerivedValidator[Short] = DerivedValidator(Validator[Short])
  implicit val unitValidator: DerivedValidator[Unit] = DerivedValidator(Validator[Unit])

  implicit def optionValidator[T](implicit v: Validator[T]): DerivedValidator[Option[T]] = DerivedValidator {
    (opt: Option[T]) => opt.map(v.validate).getOrElse(Nil)
  }

  implicit def traversableValidator[T, M[S] <: Traversable[S]](implicit v: Validator[T]): DerivedValidator[M[T]] = DerivedValidator {
    (elems: M[T]) =>
      elems.toList.zipWithIndex.flatMap { case (elem, idx) =>
        v.validate(elem).map(CollectionIndex(idx) :: _)
      }
  }

  implicit def arrayValidator[T](implicit tv: Validator[Traversable[T]]): DerivedValidator[Array[T]] = DerivedValidator {
    (elems: Array[T]) => tv.validate(elems)
  }

  implicit def mapValidator[K, V](implicit v: Validator[V]): DerivedValidator[Map[K, V]] = DerivedValidator {
    (map: Map[K, V]) =>
      map.toList.flatMap { case (key, value) =>
        v.validate(value).map(MapKey(key.toString) :: _)
      }
  }
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