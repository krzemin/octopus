package octopus.async.cats

import cats.effect.IO
import octopus.AppError
import octopus.async.AsyncValidationSpec
import octopus.async.cats.ToFutureImplicits._

class CatsEffectIOIntegrationSpec extends AsyncValidationSpec[IO] {
  implicit lazy val appError: AppError[IO] = octopus.async.cats.implicits.catsAppError
}
