package octopus.scalaz

import octopus.{App, AsyncValidationSpec}
import octopus.example.domain._
import scalaz.effect.IO

import scala.concurrent.Future

class ScalazIntegrationSpec extends AsyncValidationSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = Future(mval.unsafePerformIO())

  override implicit def app: App[IO] = octopus.async.scalaz.scalazIO
}
