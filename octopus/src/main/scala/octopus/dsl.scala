package octopus

import shapeless.{::, Generic, HNil}

import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

object dsl {

  type Validator[T] = octopus.Validator[T]
  val Validator = octopus.Validator

  type AsyncValidator[M[_], T] = octopus.AsyncValidator[M, T]
  val AsyncValidator = octopus.AsyncValidator

  implicit class ValidatorOps[T](val v: Validator[T]) extends AnyVal {

    def compose(v2: Validator[T]): Validator[T] =
      (obj: T) => v.validate(obj) ++ v2.validate(obj)

    def composeSuper[U >: T](v2: Validator[U]): Validator[T] =
      (obj: T) => v.validate(obj) ++ v2.validate(obj)

    def composeDerived(implicit dv: DerivedValidator[T]): Validator[T] =
      compose(dv.v)

    def comap[U](f: U => T): Validator[U] =
      (value: U) => v.validate(f(value))

    def rule(pred: T => Boolean, whenInvalid: String): Validator[T] =
      compose(ValidationRules.rule(pred, whenInvalid))

    def rule[F](selector: T => F, pred: F => Boolean, whenInvalid: String): Validator[T] =
      macro DslMacros.ruleFieldSelector[T, F]

    def ruleVC[V](pred: V => Boolean, whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
      compose(ValidationRules.ruleVC(pred, whenInvalid))

    def ruleCatchOnly[E <: Throwable : ClassTag](pred: T => Boolean,
                                                 whenInvalid: String,
                                                 whenCaught: E => String): Validator[T] =
      compose(ValidationRules.ruleCatchOnly(pred, whenInvalid, whenCaught))

    def ruleCatchNonFatal(pred: T => Boolean,
                          whenInvalid: String,
                          whenCaught: Throwable => String): Validator[T] =
      compose(ValidationRules.ruleCatchNonFatal(pred, whenInvalid, whenCaught))

    def ruleTry(pred: T => Try[Boolean], whenInvalid: String, whenFailure: Throwable => String): Validator[T] =
      compose(ValidationRules.ruleTry(pred, whenInvalid, whenFailure))

    def ruleEither(pred: T => Either[String, Boolean], whenInvalid: String): Validator[T] =
      compose(ValidationRules.ruleEither(pred, whenInvalid))

    def ruleOption(pred: T => Option[Boolean], whenInvalid: String, whenNone: String): Validator[T] =
      compose(ValidationRules.ruleOption(pred, whenInvalid, whenNone))

    def async[F[_]: App]: AsyncValidatorAsyncOps[F, T] =
      new AsyncValidatorAsyncOps[F, T](AsyncValidator.lift[F, T](v))
  }

  class AsyncValidatorAsyncOps[M[_]: App, T](val v: AsyncValidator[M, T]) {

    def compose(v2: AsyncValidator[M, T]): AsyncValidator[M, T] =
      AsyncValidator.instance[M, T] { (obj: T) =>
        App[M].map2(v.validate(obj), v2.validate(obj)) {
          case (e1, e2) => e1 ++ e2
        }
      }

    def composeSuper[U >: T](v2: AsyncValidator[M, U]): AsyncValidator[M, T] =
      AsyncValidator.instance { (obj: T) =>
        App[M].map2(v.validate(obj), v2.validate(obj)) {
          case (e1, e2) => e1 ++ e2
        }
      }

    def composeDerived(implicit dav: DerivedAsyncValidator[M, T]): AsyncValidator[M, T] =
      compose(dav.av)

    def comap[U](f: U => T): AsyncValidator[M, U] =
      AsyncValidator.instance[M, U] { (value: U) =>
        v.validate(f(value))
      }

    def rule(asyncPred: T => M[Boolean], whenInvalid: String): AsyncValidator[M, T] =
      compose(AsyncValidationRules.rule(asyncPred, whenInvalid))

    def rule[G](selector: T => G, pred: G => M[Boolean], whenInvalid: String): AsyncValidator[M, T] =
      macro DslMacros.ruleFieldSelectorAsync[M, T, G]

    def ruleVC[V](asyncPred: V => M[Boolean], whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[M, T] =
      compose(AsyncValidationRules.ruleVC(asyncPred, whenInvalid))

    def ruleCatchOnly[E <: Throwable : ClassTag](asyncPred: T => M[Boolean],
                                                 whenInvalid: String,
                                                 whenCaught: E => String): AsyncValidator[M, T] =
      compose(AsyncValidationRules.ruleCatchOnly(asyncPred, whenInvalid, whenCaught))

    def ruleCatchNonFatal(asyncPred: T => M[Boolean],
                          whenInvalid: String,
                          whenCaught: Throwable => String): AsyncValidator[M, T] =
      compose(AsyncValidationRules.ruleCatchNonFatal(asyncPred, whenInvalid, whenCaught))

    def ruleEither(asyncPred: T => M[Either[String, Boolean]],
                   whenInvalid: String): AsyncValidator[M, T] =
      compose(AsyncValidationRules.ruleEither(asyncPred, whenInvalid))

    def ruleOption(asyncPred: T => M[Option[Boolean]],
                   whenInvalid: String,
                   whenNone: String): AsyncValidator[M, T] =
      compose(AsyncValidationRules.ruleOption(asyncPred, whenInvalid, whenNone))

  }

  implicit class AsyncValidatorSyncOps[M[_]: App, T](val v: AsyncValidator[M, T]) {

    def async: AsyncValidatorAsyncOps[M, T] =
      new AsyncValidatorAsyncOps[M, T](v)

    def compose(v2: Validator[T]): AsyncValidator[M, T] =
      AsyncValidator.instance { (obj: T) =>
        App[M].map(v.validate(obj)) {
          _ ++ v2.validate(obj)
        }
      }

    def composeSuper[U >: T](v2: Validator[U]): AsyncValidator[M, T] =
      AsyncValidator.instance { (obj: T) =>
        App[M].map(v.validate(obj)) {
          _ ++ v2.validate(obj)
        }
      }

    def composeDerived(implicit dv: DerivedValidator[T]): AsyncValidator[M, T] =
      compose(dv.v)

    def comap[U](f: U => T): AsyncValidator[M, U] =
      AsyncValidator.instance[M, U] { (value: U) =>
          v.validate(f(value))
      }

    def rule(pred: T => Boolean, whenInvalid: String): AsyncValidator[M, T] =
      compose(ValidationRules.rule(pred, whenInvalid))

    def rule[G](selector: T => G, pred: G => Boolean, whenInvalid: String): AsyncValidator[M, T] =
      macro DslMacros.ruleFieldSelectorSync[M, T, G]

    def ruleVC[V](pred: V => Boolean, whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[M, T] =
      compose(ValidationRules.ruleVC(pred, whenInvalid))

    def ruleCatchOnly[E <: Throwable : ClassTag](pred: T => Boolean,
                                                 whenInvalid: String,
                                                 whenCaught: E => String): AsyncValidator[M, T] =
      compose(ValidationRules.ruleCatchOnly(pred, whenInvalid, whenCaught))

    def ruleCatchNonFatal(pred: T => Boolean,
                          whenInvalid: String,
                          whenCaught: Throwable => String): AsyncValidator[M, T] =
      compose(ValidationRules.ruleCatchNonFatal(pred, whenInvalid, whenCaught))

    def ruleEither(pred: T => Either[String, Boolean],
                   whenInvalid: String): AsyncValidator[M, T] =
      compose(ValidationRules.ruleEither(pred, whenInvalid))

    def ruleOption(pred: T => Option[Boolean],
                   whenInvalid: String,
                   whenNone: String): AsyncValidator[M, T] =
      compose(ValidationRules.ruleOption(pred, whenInvalid, whenNone))
  }
}
