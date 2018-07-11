package octopus.async.cats

import cats.effect.IO
import octopus.App
import octopus.example.domain._
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.concurrent.Future

class CatsEffectIntegrationSpec
  extends AsyncWordSpec
    with MustMatchers
    with BaseAsyncMonadSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = mval.unsafeToFuture()

  "Cats effect Integration" should {
    behave like validateSimpleEmail(App[IO])
  }

  "Cats effect validation result combined with base scala future async validation" should {
    behave like validateResultWithAsyncValidator(App[IO])
  }
}
