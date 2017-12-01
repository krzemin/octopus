package octopus

import scala.concurrent.{ExecutionContext, Future}

trait AsyncValidator[T] {

  def validate(obj: T)
              (implicit ec: ExecutionContext): Future[List[ValidationError]]
}

object AsyncValidator extends AsyncValidatorDerivation {

  def instance[T](f: (T, ExecutionContext) => Future[List[ValidationError]]): AsyncValidator[T] =
    new AsyncValidator[T] {
      def validate(obj: T)(implicit ec: ExecutionContext): Future[List[ValidationError]] =
        f(obj, ec)
    }

  def lift[T](v: Validator[T]): AsyncValidator[T] =
    instance { (obj: T, _: ExecutionContext) =>
      Future.successful(v.validate(obj))
    }

  def apply[T]: AsyncValidator[T] =
    lift(Validator.apply[T])

  def invalid[T](error: String): AsyncValidator[T] =
    lift(Validator.invalid(error))
}
