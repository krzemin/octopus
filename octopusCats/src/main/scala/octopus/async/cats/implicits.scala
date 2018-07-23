package octopus.async.cats

import _root_.cats.ApplicativeError
import octopus.AppError

import scala.language.higherKinds

object implicits {

  implicit def catsAppError[M[_]](implicit ae: ApplicativeError[M, Throwable]): AppError[M] = new AppError[M] {

    def pure[A](a: A): M[A] =
      ae.pure(a)

    def map[A, B](ma: M[A])(f: A => B): M[B] =
      ae.map(ma)(f)

    def map2[A, B, C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C] =
      ae.map2(ma, mb)(f)

    def recover[A, B <: A](ma: M[A], f: PartialFunction[Throwable, B]): M[A] =
      ae.recover(ma)(f)
  }
}
