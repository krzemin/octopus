package octopus

import shapeless.{::, Generic, HNil}


trait Normalizer[T] {

  def normalize(obj: T): T
}

object Normalizer extends NormalizerDerivation {

  def define[T](normalize: T => T): Normalizer[T] = new Normalizer[T] {
    @inline def normalize(obj: T): T = normalize(obj)
  }

  def defineVC[T, V](f: V => V)
                    (implicit gen: Generic.Aux[T, V :: HNil]): Normalizer[T] = new Normalizer[T] {
    @inline def normalize(obj: T): T =
      gen.from(f(gen.to(obj).head) :: HNil)
  }

  def identity[T]: Normalizer[T] = new Normalizer[T] {
    @inline def normalize(obj: T): T = obj
  }
}
