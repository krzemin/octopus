package octopus.async.scalaz

import octopus.AppError
import octopus.async.AsyncValidationSpec
import octopus.async.scalaz.ToFutureImplicits._
import scalaz.concurrent.Task

class ScalazTaskIntegrationSpec extends AsyncValidationSpec[Task] {
  implicit lazy val appError: AppError[Task] = octopus.async.scalaz.instances.scalazAppError
}
