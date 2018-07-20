package octopus.scalaz

import octopus.ToFuture
import scalaz.effect.IO

import scala.concurrent.Future

object IOToFuture {
  implicit val iOToFuture = new ToFuture[IO] {
    def toFuture[A](value: IO[A]): Future[A] = Future.successful(value.unsafePerformIO())
  }
}
