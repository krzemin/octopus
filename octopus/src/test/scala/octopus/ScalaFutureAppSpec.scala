package octopus

import scala.concurrent.Future

import ToFuture._

class ScalaFutureAppSpec extends AsyncValidationSpec[Future] {
  override implicit def app: AppError[Future] = AppError.futureAppError(executionContext)
}
