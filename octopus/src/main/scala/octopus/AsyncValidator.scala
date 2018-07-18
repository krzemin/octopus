package octopus

import scala.concurrent.Future
import scala.language.higherKinds

private[octopus] object AsyncValidator {
  def apply[T](implicit appError: AppError[Future]): octopus.dsl.AsyncValidator[T] =
    AsyncValidatorM.lift[Future, T](Validator.apply[T])
}

trait AsyncValidatorM[M[_], T] {
  def validate(obj: T)(implicit appError: AppError[M]): M[List[ValidationError]]
}

object AsyncValidatorM {

  def instance[M[_], T](f: T => M[List[ValidationError]]): AsyncValidatorM[M, T] = {
    new AsyncValidatorM[M, T] {
      def validate(obj: T)(implicit appError: AppError[M]): M[List[ValidationError]] =
        f(obj)
    }
  }

  def lift[M[_]: AppError, T](v: Validator[T]): AsyncValidatorM[M, T] =
    instance { obj =>
      AppError[M].pure(v.validate(obj))
    }

  def apply[M[_]: AppError, T]: AsyncValidatorM[M, T] =
    lift(Validator.apply[T])

  def invalid[M[_]: AppError, T](error: String): AsyncValidatorM[M, T] =
    lift(Validator.invalid(error))

  implicit def fromDerived[F[_], T](implicit dav: DerivedAsyncValidator[F, T]): AsyncValidatorM[F, T] = dav.av
}
