package octopus

import shapeless.{::, Generic, HNil}


trait Normalizer[T] {

  def normalize(obj: T): T
}

object Normalizer {

  def define[T](normalize: T => T): Normalizer[T] = new Normalizer[T] {
    @inline def normalize(obj: T): T = normalize(obj)
  }

  def defineVC[T, V](normalize: V => V)
                    (implicit gen: Generic.Aux[T, V :: HNil]): Normalizer[T] =
    define((t: T) => gen.from(normalize(gen.to(t).head) :: HNil))

  def identity[T]: Normalizer[T] = define(t => t)

}
