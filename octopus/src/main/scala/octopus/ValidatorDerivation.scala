package octopus

import shapeless.labelled.FieldType
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, LabelledGeneric, Lazy, Witness}


trait ValidatorDerivation extends LowPriorityValidatorDerivation {

  implicit val stringValidator: Validator[String] = Validator[String]
  implicit val intValidator: Validator[Int] = Validator[Int]
  implicit val boolValidator: Validator[Boolean] = Validator[Boolean]
  implicit val doubleValidator: Validator[Double] = Validator[Double]
  implicit val floatValidator: Validator[Float] = Validator[Float]
  implicit val charValidator: Validator[Char] = Validator[Char]
  implicit val byteValidator: Validator[Byte] = Validator[Byte]
  implicit val shortValidator: Validator[Short] = Validator[Short]
  implicit val unitValidator: Validator[Unit] = Validator[Unit]

  implicit def optionValidator[T](implicit v: Validator[T]): Validator[Option[T]] =
    (opt: Option[T]) => opt.map(v.validate).getOrElse(Nil)

  implicit def traversableValidator[T, M[_]](implicit ev: M[T] <:< Traversable[T],
                                             v: Validator[T]): Validator[M[T]] =
    (elems: M[T]) =>
      elems.toList.zipWithIndex.flatMap { case (elem, idx) =>
        v.validate(elem).map(CollectionIndex(idx) :: _)
      }

  implicit def arrayValidator[T](implicit tv: Validator[Traversable[T]]): Validator[Array[T]] =
    (elems: Array[T]) => tv.validate(elems)

  implicit def mapValidator[K, V](implicit v: Validator[V]): Validator[Map[K, V]] =
    (map: Map[K, V]) => map.toList.flatMap { case (key, value) =>
      v.validate(value).map(MapKey(key.toString) :: _)
    }
}

trait LowPriorityValidatorDerivation {

  implicit val hnilValidator: Validator[HNil] = Validator[HNil]

  implicit def hconsValidator[L <: Symbol, H, T <: HList](implicit label: Witness.Aux[L],
                                                          hv: Lazy[Validator[H]],
                                                          tv: Validator[T]): Validator[FieldType[L, H] :: T] =
    (hlist: FieldType[L, H] :: T) =>
      hv.value.validate(hlist.head).map(FieldLabel(label.value) :: _) ++ tv.validate(hlist.tail)

  implicit val cnilValidator: Validator[CNil] = (_: CNil) => null

  implicit def coproductValidator[L <: Symbol, H, T <: Coproduct](implicit hv: Lazy[Validator[H]],
                                                                  tv: Validator[T]): Validator[FieldType[L, H] :+: T] = {
    case Inl(head) => hv.value.validate(head)
    case Inr(tail) => tv.validate(tail)
  }

  implicit def genValidator[T, Repr](implicit gen: LabelledGeneric.Aux[T, Repr],
                                     v: Lazy[Validator[Repr]]): Validator[T] =
    (obj: T) => v.value.validate(gen.to(obj))
}