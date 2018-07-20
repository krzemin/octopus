package octopus.scalaz

import octopus.scalaz.IOToFuture._
import octopus.{AppError, AsyncValidationSpec}
import scalaz.effect.IO

class ScalazIntegrationSpec extends AsyncValidationSpec[IO] {
  override implicit def app: AppError[IO] = octopus.async.scalaz.scalazIOAppError
}
