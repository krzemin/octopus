package octopus

import magnolia._

import scala.concurrent.Future
import scala.language.higherKinds
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[octopus] object AsyncValidator extends Serializable {
//  AsyncValidatorM.Derivation[Future]
  def apply[T](implicit appError: AppError[Future]): octopus.dsl.AsyncValidator[T] =
    AsyncValidatorM.lift[Future, T](Validator.apply[T])
}

trait AsyncValidatorM[M[_], T] {
  def validate(obj: T)(implicit appError: AppError[M]): M[List[ValidationError]]
}

object AsyncValidatorM extends Serializable {

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

//  implicit def fromDerived[M[_], T](implicit dav: DerivedAsyncValidator[M, T]): AsyncValidatorM[M, T] = dav.av

  implicit def derived[M[_], T]: AsyncValidatorM[M, T] =
    new AsyncValidatorMDerivation[M].derived[T]
}

class AsyncValidatorMDerivation[M[_]] extends LowPriorityAsyncValidatorDerivation {

  type Typeclass[T] = AsyncValidatorM[M, T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    new AsyncValidatorM[M, T] {
      def validate(obj: T)(implicit appError: AppError[M]): M[List[ValidationError]] = {
        caseClass.parameters.foldLeft(appError.pure(List.empty[ValidationError])) {
          case (acc, param) =>
            appError.map2(
              acc,
              param.typeclass.validate(param.dereference(obj))(appError)
            )(_ ++ _)
        }
      }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    new AsyncValidatorM[M, T] {
      def validate(obj: T)(implicit appError: AppError[M]): M[List[ValidationError]] = {
        sealedTrait.dispatch(obj) { subtype =>
          subtype.typeclass.validate(subtype.cast(obj))
        }
      }
    }

  implicit def derived[T]: Typeclass[T] = macro Magnolia.gen[T]
}

trait LowPriorityAsyncValidatorDerivation extends Serializable {

  implicit def fromSyncValidator[M[_]: AppError, T](implicit v: Validator[T]): AsyncValidatorM[M, T] =
    AsyncValidatorM.lift(v)
}
