package octopus.async.cats

import cats.effect.IO
import octopus.AsyncValidationSpec
import octopus.async.cats.implicits._
import octopus.async.cats.toFuture._

class CatsEffectIOIntegrationSpec extends AsyncValidationSpec[IO]
