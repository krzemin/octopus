package octopus.async

import _root_.scalaz.Scalaz._
import _root_.scalaz.effect.IO
import octopus.AppError

package object scalaz {

  implicit val scalazIOAppError: AppError[IO] = new AppError[IO] {
    override def pure[A](a: A): IO[A] = IO(a)

    override def map2[A, B, C](first: IO[A], second: IO[B])(combine: (A, B) => C): IO[C] =
      (first |@| second)(combine)

    override def recover[A, B <: A](app: IO[A], f: Throwable => B): IO[A] =
      app.except( t => IO(f(t)) )

    override def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }
}
