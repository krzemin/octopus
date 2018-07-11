package octopus

import scala.concurrent.{ExecutionContext, Future}

trait App[M[_]] {
  def pure[A](a: A): M[A]
  def map2[A, B, C](first: M[A], second: M[B])(combine: (A, B) => C): M[C]
  def recover[A, B <: A](app: M[A], f: Throwable => B): M[A]
  def map[A, B](fa: M[A])(f: A => B): M[B] = {
    map2(fa, pure(true))((a, _) => f(a))
  }
}

object App extends LowPriorityAppImplicits {
  def apply[M[_]](implicit a: App[M]): App[M] = a
}

trait LowPriorityAppImplicits {

  implicit def futureApp(implicit ec: ExecutionContext): App[Future] = new App[Future] {
    override def pure[A](a: A): Future[A] = Future.successful(a)

    override def map2[A, B, C](first: Future[A], second: Future[B])(combine: (A, B) => C): Future[C] =
      // Reimplement to Future.zipWith after dropping scala 2.11 support
      first.zip(second).map(combine.tupled)

    override def recover[A, B <: A](app: Future[A], f: Throwable => B): Future[A] = app.recover {
      case t: Throwable => f(t)
    }
  }
}
