package octopus

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ScalaFutureAsyncValidationSpec extends AsyncValidationSpec[Future]
