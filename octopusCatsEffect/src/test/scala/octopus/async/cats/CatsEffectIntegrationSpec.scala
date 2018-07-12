package octopus.async.cats

import cats.effect.IO
import octopus.{App, AsyncValidationSpec}
import octopus.example.domain._

import scala.concurrent.Future

class CatsEffectIntegrationSpec extends AsyncValidationSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = mval.unsafeToFuture()

  override implicit def app: App[IO] = octopus.async.cats.catsIO
}
