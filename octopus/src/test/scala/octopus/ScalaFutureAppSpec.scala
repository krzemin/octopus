package octopus

import octopus.example.domain.BaseAsyncMonadSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{AsyncWordSpec, MustMatchers}

import scala.concurrent.Future

class ScalaFutureAppSpec
  extends AsyncWordSpec
    with MustMatchers
    with BaseAsyncMonadSpec[Future]
    with ScalaFutures
    with IntegrationPatience {

  override def extractValueFrom[A](mval: Future[A]): Future[A] = mval

  "Base Scala future app implementation" should {
    behave like validateSimpleEmail(App[Future])
  }

  "Validation result combined with base scala future async validation" should {
    behave like validateResultWithAsyncValidator(App[Future])
  }

}
