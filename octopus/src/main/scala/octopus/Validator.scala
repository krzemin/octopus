package octopus

trait Validator[T] extends Serializable {
  def validate(obj: T): List[ValidationError]
}

object Validator extends Serializable {

  def instance[T](f: T => List[ValidationError]): Validator[T] = f(_)
  def apply[T]: Validator[T] = _ => Nil
  def invalid[T](error: String): Validator[T] = _ => List(ValidationError(error))
  def derived[T](implicit dv: DerivedValidator[T]): Validator[T] = dv.v

  implicit val stringValidator: Validator[String] = Validator[String]
  implicit val intValidator: Validator[Int] = Validator[Int]
  implicit val longValidator: Validator[Long] = Validator[Long]
  implicit val boolValidator: Validator[Boolean] = Validator[Boolean]
  implicit val doubleValidator: Validator[Double] = Validator[Double]
  implicit val floatValidator: Validator[Float] = Validator[Float]
  implicit val charValidator: Validator[Char] = Validator[Char]
  implicit val byteValidator: Validator[Byte] = Validator[Byte]
  implicit val shortValidator: Validator[Short] = Validator[Short]
  implicit val unitValidator: Validator[Unit] = Validator[Unit]

  implicit def optionValidator[T](implicit v: Validator[T]): Validator[Option[T]] =
    _.fold(List.empty[ValidationError])(v.validate)

  implicit def iterableValidator[T, M[S] <: Iterable[S]](implicit v: Validator[T]): Validator[M[T]] =
    _.toList.zipWithIndex.flatMap { case (elem, idx) =>
      v.validate(elem).map(CollectionIndex(idx) :: _)
    }

  implicit def arrayValidator[T](implicit tv: Validator[Iterable[T]]): Validator[Array[T]] =
    tv.validate(_)

  implicit def mapValidator[K, V](implicit v: Validator[V]): Validator[Map[K, V]] =
    _.toList.flatMap { case (key, value) =>
      v.validate(value).map(MapKey(key.toString) :: _)
    }

  implicit def fromDerivedValidator[T](implicit dv: DerivedValidator[T]): Validator[T] = dv.v
}
