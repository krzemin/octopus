package octopus

import scala.concurrent.{ExecutionContext, Future}
import language.higherKinds
import scala.annotation.implicitNotFound

@implicitNotFound("Implicit instance for octopus.AppError[${M}] not found in scope!")
trait AppError[M[_]] {
  def pure[A](a: A): M[A]
  def map[A, B](ma: M[A])(f: A => B): M[B]
  def map2[A, B, C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C]
  def recover[A, B <: A](ma: M[A], f: PartialFunction[Throwable, B]): M[A]
}

object AppError {
  def apply[M[_]](implicit a: AppError[M]): AppError[M] = a

  implicit def futureAppError(implicit ec: ExecutionContext): AppError[Future] = new AppError[Future] {
    def pure[A](a: A): Future[A] =
      Future.successful(a)

    def map[A, B](fa: Future[A])(f: A => B): Future[B] =
      fa.map(f)

    def map2[A, B, C](fa: Future[A], fb: Future[B])(f: (A, B) => C): Future[C] =
      fa.zip(fb).map { case (a, b) => f(a, b) }

    def recover[A, B <: A](fa: Future[A], f: PartialFunction[Throwable, B]): Future[A] =
      fa.recover(f)
  }
}
