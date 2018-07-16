package octopus.async.cats

import cats.effect.IO
import octopus.{AppError, AsyncValidationSpec}

import scala.concurrent.Future

class CatsEffectIntegrationSpec extends AsyncValidationSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = mval.unsafeToFuture()

  override implicit def app: AppError[IO] = octopus.async.cats.catsIOAppError
}
