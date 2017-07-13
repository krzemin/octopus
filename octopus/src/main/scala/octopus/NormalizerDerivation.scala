package octopus

import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, Lazy}

import scala.collection.generic.CanBuildFrom


trait NormalizerDerivation extends LowPriorityNormalizerDerivation {

  implicit val stringNormalizer: Normalizer[String] = Normalizer.identity[String]
  implicit val intNormalizer: Normalizer[Int] = Normalizer.identity[Int]
  implicit val boolNormalizer: Normalizer[Boolean] = Normalizer.identity[Boolean]
  implicit val doubleNormalizer: Normalizer[Double] = Normalizer.identity[Double]
  implicit val floatNormalizer: Normalizer[Float] = Normalizer.identity[Float]
  implicit val charNormalizer: Normalizer[Char] = Normalizer.identity[Char]
  implicit val byteNormalizer: Normalizer[Byte] = Normalizer.identity[Byte]
  implicit val shortNormalizer: Normalizer[Short] = Normalizer.identity[Short]
  implicit val unitNormalizer: Normalizer[Unit] = Normalizer.identity[Unit]

  implicit def optionNormalizer[T](implicit n: Normalizer[T]): Normalizer[Option[T]] =
    Normalizer.define(_.map(n.normalize))

  implicit def traversableNormalizer[T, M[_]](implicit ev: M[T] <:< Traversable[T],
                                              n: Normalizer[T],
                                              cbf: CanBuildFrom[Traversable[T], T, M[T]]): Normalizer[M[T]] =
    Normalizer.define(_.map(n.normalize))

  implicit def arrayNormalizer[T](implicit n: Normalizer[T],
                                  cbf: CanBuildFrom[Array[T], T, Array[T]]): Normalizer[Array[T]] =
    Normalizer.define(_.map(n.normalize))

  implicit def mapNormalizer[K, V](implicit n: Normalizer[V]): Normalizer[Map[K, V]] =
    Normalizer.define(_.mapValues(n.normalize))
}

trait LowPriorityNormalizerDerivation {

  implicit val hnilNormalizer: Normalizer[HNil] = Normalizer.identity

  implicit def hconsNormalizer[H, T <: HList](implicit hn: Lazy[Normalizer[H]],
                                              tn: Normalizer[T]): Normalizer[H :: T] =
    Normalizer.define(hlist => hn.value.normalize(hlist.head) :: tn.normalize(hlist.tail))

  // $COVERAGE-OFF$
  implicit val cnilNormalizer: Normalizer[CNil] =
    Normalizer.define(_ => null)
  // $COVERAGE-ON$

  implicit def coproductNormalizer[H, T <: Coproduct](implicit hn: Lazy[Normalizer[H]],
                                                      tn: Normalizer[T]): Normalizer[H :+: T] =
    Normalizer.define {
      case Inl(head) => Inl(hn.value.normalize(head))
      case Inr(tail) => Inr(tn.normalize(tail))
    }

  implicit def genNormalizer[T, Repr](implicit gen: Generic.Aux[T, Repr],
                                      n: Lazy[Normalizer[Repr]]): Normalizer[T] =
    Normalizer.define(t => gen.from(n.value.normalize(gen.to(t))))
}