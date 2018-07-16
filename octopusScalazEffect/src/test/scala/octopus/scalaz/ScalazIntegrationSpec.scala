package octopus.scalaz

import octopus.{AppError, AsyncValidationSpec}
import scalaz.effect.IO

import scala.concurrent.Future

class ScalazIntegrationSpec extends AsyncValidationSpec[IO] {

  override def extractValueFrom[A](mval: IO[A]): Future[A] = Future(mval.unsafePerformIO())

  override implicit def app: AppError[IO] = octopus.async.scalaz.scalazIOAppError
}
