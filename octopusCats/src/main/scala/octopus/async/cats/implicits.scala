package octopus.async.cats

import _root_.cats.ApplicativeError
import octopus.AppError

import scala.language.higherKinds

object implicits {

  implicit def catsAppError[M[_]](implicit M: ApplicativeError[M, Throwable]): AppError[M] = new AppError[M] {

    def pure[A](a: A): M[A] =
      M.pure(a)

    def failed[A](why: Throwable): M[A] =
      M.raiseError(why)

    def map[A, B](ma: M[A])(f: A => B): M[B] =
      M.map(ma)(f)

    def map2[A, B, C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C] =
      M.map2(ma, mb)(f)

    def recover[A, B <: A](ma: M[A], f: PartialFunction[Throwable, B]): M[A] =
      M.recover(ma)(f)
  }
}
