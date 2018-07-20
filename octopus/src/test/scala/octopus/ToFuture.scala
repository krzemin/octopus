package octopus

import scala.concurrent.Future

trait ToFuture[F[_]] {
  def toFuture[A](value: F[A]): Future[A]
}

object ToFuture {
  implicit val futureToFuture = new ToFuture[Future] {
    def toFuture[A](value: Future[A]): Future[A] = value
  }
}
