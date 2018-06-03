package octopus

import scala.language.higherKinds

trait AsyncValidator[M[_], T] {
  def validate(obj: T)(implicit app: App[M]): M[List[ValidationError]]
}

object AsyncValidator {

  def instance[M[_], T](f: T => M[List[ValidationError]]): AsyncValidator[M, T] = {
    new AsyncValidator[M, T] {
      def validate(obj: T)(implicit app: App[M]): M[List[ValidationError]] =
        f(obj)
    }
  }

  def lift[M[_]: App, T](v: Validator[T]): AsyncValidator[M, T] =
    instance { obj =>
      App[M].pure(v.validate(obj))
    }

  def apply[M[_]: App, T]: AsyncValidator[M, T] =
    lift(Validator.apply[T])

  def invalid[M[_]: App, T](error: String): AsyncValidator[M, T] =
    lift(Validator.invalid(error))

  implicit def fromDerivedAsyncValidator[F[_], T](implicit dav: DerivedAsyncValidator[F, T]): AsyncValidator[F, T] = dav.av
}
