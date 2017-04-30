package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.Selector

import scala.reflect.ClassTag
import scala.util.Try

object dsl {

  type Validator[T] = octopus.Validator[T]
  val Validator = octopus.Validator

  implicit class ValidatorOps[T](val v: Validator[T]) extends AnyVal {

    def compose(v2: Validator[T]): Validator[T] =
      (obj: T) => v.validate(obj) ++ v2.validate(obj)

    def rule(pred: T => Boolean, whenInvalid: String): Validator[T] =
      v compose Validator.rule(pred, whenInvalid)

    def ruleVC[V](pred: V => Boolean, whenInvalid: String)
                 (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
      v compose Validator.ruleVC(pred, whenInvalid)

    def ruleField[R <: HList, U]
    (field: Witness, pred: U => Boolean, whenInvalid: String)
    (implicit ev: field.T <:< Symbol,
     gen: LabelledGeneric.Aux[T, R],
     sel: Selector.Aux[R, field.T, U]): Validator[T] =
      v compose Validator.ruleField(field, pred, whenInvalid)

    def ruleCatchOnly[E <: Throwable : ClassTag](pred: T => Boolean, whenInvalid: String, whenCaught: E => String): Validator[T] =
      v compose Validator.ruleCatchOnly(pred, whenInvalid, whenCaught)

    def ruleCatchNonFatal(pred: T => Boolean, whenInvalid: String, whenCaught: Throwable => String): Validator[T] =
      v compose Validator.ruleCatchNonFatal(pred, whenInvalid, whenCaught)

    def ruleTry(pred: T => Try[Boolean], whenInvalid: String, whenFailure: Throwable => String): Validator[T] =
      v compose Validator.ruleTry(pred, whenInvalid, whenFailure)

    def ruleEither(pred: T => Either[String, Boolean], whenInvalid: String): Validator[T] =
      v compose Validator.ruleEither(pred, whenInvalid)

    def ruleOption(pred: T => Option[Boolean], whenInvalid: String, whenNone: String): Validator[T] =
      v compose Validator.ruleOption(pred, whenInvalid, whenNone)
  }
}
