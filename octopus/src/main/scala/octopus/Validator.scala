package octopus

import magnolia._

import scala.language.experimental.macros

trait Validator[T] extends Serializable{

  def validate(obj: T): List[ValidationError]
}

object Validator extends Serializable {

  def instance[T](f: T => List[ValidationError]): Validator[T] =
    (obj: T) => f(obj)

  def apply[T]: Validator[T] = (_: T) => Nil

  def invalid[T](error: String): Validator[T] = (_: T) => List(ValidationError(error))

//  def derived[T](implicit dv: DerivedValidator[T]): Validator[T] =
//    dv.v
//
//  implicit def fromDerivedValidator[T](implicit dv: DerivedValidator[T]): Validator[T] = dv.v

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
    (map: Map[K, V]) =>
      map.toList.flatMap { case (key, value) =>
        v.validate(value).map(MapKey(key.toString) :: _)
      }

  type Typeclass[T] = Validator[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = (obj: T) => {
    caseClass.parameters
      .flatMap { param =>
        param.typeclass.validate(param.dereference(obj))
          .map(FieldLabel(Symbol(param.label)) :: _)
      }
      .toList
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = (obj: T) => {
    sealedTrait.dispatch(obj) { subtype =>
      subtype.typeclass.validate(subtype.cast(obj))
    }
  }

  implicit def derived[T]: Typeclass[T] = macro Magnolia.gen[T]
}
