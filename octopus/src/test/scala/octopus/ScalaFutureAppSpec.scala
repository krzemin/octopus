package octopus

import scala.concurrent.Future

class ScalaFutureAppSpec
  extends AsyncValidationSpec[Future] {

  override def extractValueFrom[A](mval: Future[A]): Future[A] = mval

  override implicit def app: App[Future] = App.futureApp(executionContext)
}
