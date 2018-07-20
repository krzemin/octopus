package octopus.async.cats

import cats.effect.IO
import octopus.ToFuture

import scala.concurrent.Future

object IOToFuture {
  implicit val iOToFuture = new ToFuture[IO] {
    def toFuture[A](value: IO[A]): Future[A] = value.unsafeToFuture()
  }
}
