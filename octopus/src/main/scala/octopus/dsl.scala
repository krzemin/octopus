package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.Selector

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.util.Try

object dsl {

  type Validator[T] = octopus.Validator[T]
  val Validator = octopus.Validator

  type AsyncValidator[T] = octopus.AsyncValidator[T]
  val AsyncValidator = octopus.AsyncValidator

  implicit class ValidatorOps[T](val v: Validator[T]) extends AnyVal {

    def compose(v2: Validator[T]): Validator[T] =
      (obj: T) => v.validate(obj) ++ v2.validate(obj)

    def composeSuper[U >: T](v2: Validator[U]): Validator[T] =
      (obj: T) => v.validate(obj) ++ v2.validate(obj)

    def composeDerived(implicit dv: DerivedValidator[T]): Validator[T] =
      (obj: T) => v.validate(obj) ++ dv.v.validate(obj)

    def comap[U](f: U => T): Validator[U] =
      (value: U) => v.validate(f(value))

    def rule(pred: T => Boolean, whenInvalid: String): Validator[T] =
      compose(ValidationRules.rule(pred, whenInvalid))

    def rule[F](selector: T => F, pred: F => Boolean, whenInvalid: String): Validator[T] =
      macro DslMacros.ruleFieldSelector[T, F]

    def ruleVC[V](pred: V => Boolean, whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
      compose(ValidationRules.ruleVC(pred, whenInvalid))

    def ruleField[R <: HList, U](field: Witness, pred: U => Boolean, whenInvalid: String)
                                (implicit ev: field.T <:< Symbol,
                                 gen: LabelledGeneric.Aux[T, R],
                                 sel: Selector.Aux[R, field.T, U]): Validator[T] =
      compose(ValidationRules.ruleField(field, pred, whenInvalid))

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


    def async: AsyncValidatorAsyncOps[T] =
      new AsyncValidatorAsyncOps[T](AsyncValidator.lift(v))
  }

  class AsyncValidatorAsyncOps[T](val v: AsyncValidator[T]) extends AnyVal {

    def compose(v2: AsyncValidator[T]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        (v.validate(obj)(ec) zip v2.validate(obj)(ec))
          .map { case (e1, e2) => e1 ++ e2 }(ec)
      }

    def composeSuper[U >: T](v2: AsyncValidator[U]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        (v.validate(obj)(ec) zip v2.validate(obj)(ec))
          .map { case (e1, e2) => e1 ++ e2 }(ec)
      }

    def composeDerived(implicit dav: DerivedAsyncValidator[T]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        (v.validate(obj)(ec) zip dav.av.validate(obj)(ec))
          .map { case (e1, e2) => e1 ++ e2 }(ec)
      }

    def comap[U](f: U => T): AsyncValidator[U] =
      AsyncValidator.instance[U] {
        (value: U, ec: ExecutionContext) =>
          v.validate(f(value))(ec)
      }

    def rule(asyncPred: T => Future[Boolean], whenInvalid: String): AsyncValidator[T] =
      compose(AsyncValidationRules.rule(asyncPred, whenInvalid))

    def rule[F](selector: T => F, pred: F => Future[Boolean], whenInvalid: String): AsyncValidator[T] =
      macro DslMacros.ruleFieldSelectorAsync[T, F]

    def ruleVC[V](asyncPred: V => Future[Boolean], whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleVC(asyncPred, whenInvalid))

    def ruleField[R <: HList, U](field: Witness, asyncPred: U => Future[Boolean], whenInvalid: String)
                                (implicit ev: field.T <:< Symbol,
                                 gen: LabelledGeneric.Aux[T, R],
                                 sel: Selector.Aux[R, field.T, U]): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleField(field, asyncPred, whenInvalid))

    def ruleCatchOnly[E <: Throwable : ClassTag](asyncPred: T => Future[Boolean],
                                                 whenInvalid: String,
                                                 whenCaught: E => String): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleCatchOnly(asyncPred, whenInvalid, whenCaught))

    def ruleCatchNonFatal(asyncPred: T => Future[Boolean],
                          whenInvalid: String,
                          whenCaught: Throwable => String): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleCatchNonFatal(asyncPred, whenInvalid, whenCaught))

    def ruleEither(asyncPred: T => Future[Either[String, Boolean]],
                   whenInvalid: String): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleEither(asyncPred, whenInvalid))

    def ruleOption(asyncPred: T => Future[Option[Boolean]],
                   whenInvalid: String,
                   whenNone: String): AsyncValidator[T] =
      compose(AsyncValidationRules.ruleOption(asyncPred, whenInvalid, whenNone))

  }

  implicit class AsyncValidatorSyncOps[T](val v: AsyncValidator[T]) extends AnyVal {

    def async: AsyncValidatorAsyncOps[T] =
      new AsyncValidatorAsyncOps[T](v)

    def compose(v2: Validator[T]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        v.validate(obj)(ec)
          .map(_ ++ v2.validate(obj))(ec)
      }

    def composeSuper[U >: T](v2: Validator[U]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        v.validate(obj)(ec)
          .map(_ ++ v2.validate(obj))(ec)
      }

    def composeDerived(implicit dav: DerivedValidator[T]): AsyncValidator[T] =
      AsyncValidator.instance { (obj: T, ec: ExecutionContext) =>
        v.validate(obj)(ec)
          .map(_ ++ dav.v.validate(obj))(ec)
      }

    def comap[U](f: U => T): AsyncValidator[U] =
      AsyncValidator.instance[U] {
        (value: U, ec: ExecutionContext) =>
          v.validate(f(value))(ec)
      }

    def rule(pred: T => Boolean, whenInvalid: String): AsyncValidator[T] =
      compose(ValidationRules.rule(pred, whenInvalid))

    def rule[F](selector: T => F, pred: F => Boolean, whenInvalid: String): AsyncValidator[T] =
      macro DslMacros.ruleFieldSelectorSync[T, F]

    def ruleVC[V](pred: V => Boolean, whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[T] =
      compose(ValidationRules.ruleVC(pred, whenInvalid))

    def ruleField[R <: HList, U](field: Witness, pred: U => Boolean, whenInvalid: String)
                                (implicit ev: field.T <:< Symbol,
                                 gen: LabelledGeneric.Aux[T, R],
                                 sel: Selector.Aux[R, field.T, U]): AsyncValidator[T] =
      compose(ValidationRules.ruleField(field, pred, whenInvalid))

    def ruleCatchOnly[E <: Throwable : ClassTag](pred: T => Boolean,
                                                 whenInvalid: String,
                                                 whenCaught: E => String): AsyncValidator[T] =
      compose(ValidationRules.ruleCatchOnly(pred, whenInvalid, whenCaught))

    def ruleCatchNonFatal(pred: T => Boolean,
                          whenInvalid: String,
                          whenCaught: Throwable => String): AsyncValidator[T] =
      compose(ValidationRules.ruleCatchNonFatal(pred, whenInvalid, whenCaught))

    def ruleEither(pred: T => Either[String, Boolean],
                   whenInvalid: String): AsyncValidator[T] =
      compose(ValidationRules.ruleEither(pred, whenInvalid))

    def ruleOption(pred: T => Option[Boolean],
                   whenInvalid: String,
                   whenNone: String): AsyncValidator[T] =
      compose(ValidationRules.ruleOption(pred, whenInvalid, whenNone))
  }
}
