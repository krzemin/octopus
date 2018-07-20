package octopus.async

import _root_.scalaz.Scalaz._
import _root_.scalaz.effect.IO
import octopus.AppError

package object scalaz {

  implicit val scalazIOAppError: AppError[IO] = new AppError[IO] {
    def pure[A](a: A): IO[A] = IO(a)

    def map2[A, B, C](first: IO[A], second: IO[B])(combine: (A, B) => C): IO[C] =
      (first |@| second)(combine)

    def recover[A, B <: A](io: IO[A], f: Throwable => B): IO[A] =
      io.except( t => IO(f(t)) )

    def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }
}
