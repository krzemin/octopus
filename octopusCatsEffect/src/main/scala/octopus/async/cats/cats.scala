package octopus.async

import _root_.cats.effect.IO
import _root_.cats.implicits._
import _root_.cats.Applicative

import octopus.AppError

package object cats {

  implicit val catsIOAppError: AppError[IO] = new AppError[IO] {
    def pure[A](a: A): IO[A] = IO(a)

    def map2[A, B, C](first: IO[A], second: IO[B])(combine: (A, B) => C): IO[C] =
      Applicative[IO].map2(first, second)(combine)

    def recover[A, B <: A](app: IO[A], f: Throwable => B): IO[A] =
      app.recover {
        case a: Throwable => f(a)
      }

    def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }
}

