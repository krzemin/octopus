package octopus.async.scalaz

import _root_.scalaz.MonadError
import octopus.AppError

import scala.language.higherKinds

object instances {

  implicit def scalazAppError[M[_]](implicit me: MonadError[M, Throwable]): AppError[M] = new AppError[M] {

    def pure[A](a: A): M[A] =
      me.point(a)

    def failed[A](why: Throwable): M[A] =
      me.raiseError(why)

    def map[A, B](ma: M[A])(f: A => B): M[B] =
      me.map(ma)(f)

    def map2[A, B, C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C] =
      me.apply2(ma, mb)(f)

    def recover[A, B <: A](ma: M[A], f: PartialFunction[Throwable, B]): M[A] =
      me.handleError(ma)(f.andThen(pure(_))) // TODO: partial function composed with pure gives pure? :>
  }
}
