package octopus

import scala.concurrent.{ExecutionContext, Future}

trait AppError[M[_]] {
  def pure[A](a: A): M[A]
  def map2[A, B, C](first: M[A], second: M[B])(combine: (A, B) => C): M[C]
  def recover[A, B <: A](app: M[A], f: Throwable => B): M[A]
  def map[A, B](fa: M[A])(f: A => B): M[B]
}

object AppError {
  def apply[M[_]](implicit a: AppError[M]): AppError[M] = a

  implicit def futureAppError(implicit ec: ExecutionContext): AppError[Future] = new AppError[Future] {
    def pure[A](a: A): Future[A] = Future.successful(a)

    def map2[A, B, C](first: Future[A], second: Future[B])(combine: (A, B) => C): Future[C] =
    // Reimplement to Future.zipWith after dropping scala 2.11 support
      first.zip(second).map(combine.tupled)

    def recover[A, B <: A](app: Future[A], f: Throwable => B): Future[A] = app.recover {
      case t: Throwable => f(t)
    }

    def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  }
}
