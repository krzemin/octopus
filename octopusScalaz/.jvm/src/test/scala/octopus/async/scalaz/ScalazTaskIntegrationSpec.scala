package octopus.async.scalaz

import octopus.AsyncValidationSpec
import octopus.async.scalaz.instances._
import octopus.async.scalaz.toFuture._
import scalaz.concurrent.Task

class ScalazTaskIntegrationSpec extends AsyncValidationSpec[Task]
