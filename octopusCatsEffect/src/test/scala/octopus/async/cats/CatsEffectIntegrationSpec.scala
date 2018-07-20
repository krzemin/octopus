package octopus.async.cats

import cats.effect.IO
import octopus.async.cats.IOToFuture._
import octopus.{AppError, AsyncValidationSpec}

class CatsEffectIntegrationSpec extends AsyncValidationSpec[IO] {
  override implicit def app: AppError[IO] = octopus.async.cats.catsIOAppError
}
