package octopus.async.cats

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import octopus.AsyncValidationSpec
import octopus.async.cats.implicits._
import octopus.async.cats.toFuture._

class MonixTaskIntegrationSpec extends AsyncValidationSpec[Task]
