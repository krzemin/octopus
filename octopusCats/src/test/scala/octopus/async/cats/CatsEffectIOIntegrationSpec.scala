package octopus.async.cats

import cats.effect.IO
import octopus.AsyncValidationSpec
import octopus.async.cats.implicits._
import octopus.async.cats.ToFuture._

class CatsEffectIOIntegrationSpec extends AsyncValidationSpec[IO]
