package octopus.async.cats

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import octopus.AppError
import octopus.async.AsyncValidationSpec
import octopus.async.cats.ToFutureImplicits._

class MonixTaskIntegrationSpec extends AsyncValidationSpec[Task] {
  implicit lazy val appError: AppError[Task] = octopus.async.cats.implicits.catsAppError
}
