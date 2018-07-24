package octopus.async

import scala.concurrent.Future
import scala.language.higherKinds

trait ToFuture[F[_]] {
  def toFuture[A](value: F[A]): Future[A]
}

object ToFuture {

  implicit val futureInstance: ToFuture[Future] = new ToFuture[Future] {
    def toFuture[A](value: Future[A]): Future[A] = value
  }

  object syntax {
    implicit class ToFutureOps[F[_]: ToFuture, A](value: F[A]) {
      def toFuture: Future[A] = implicitly[ToFuture[F]].toFuture(value)
    }
  }
}
