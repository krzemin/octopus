package octopus.async

import octopus.AppError

import scala.concurrent.Future

class ScalaFutureAsyncValidationSpec extends AsyncValidationSpec[Future] {
  implicit lazy val appError: AppError[Future] = AppError.futureAppError
}
