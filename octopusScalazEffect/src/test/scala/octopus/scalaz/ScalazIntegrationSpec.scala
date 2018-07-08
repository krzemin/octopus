package octopus.scalaz

import octopus.App
import octopus.async.scalaz._
import octopus.example.domain._
import org.scalatest.{AsyncWordSpec, MustMatchers}
import scalaz.effect.IO

import scala.concurrent.Future

class ScalazIntegrationSpec
  extends AsyncWordSpec
    with MustMatchers
    with BaseAsyncMonadSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = Future(mval.unsafePerformIO())

  "Scalaz Integration" should {
    behave like validateSimpleEmail(App[IO])
  }

  "Validation result combined with base scala future async validation" should {
    behave like validateResultWithAsyncValidator(App[IO])
  }
}
