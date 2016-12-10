package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Lazy, Witness}
import shapeless.ops.record.Selector

import scala.util.{Failure, Success, Try}

trait Validator[T] {

  def validate(obj: T): List[ValidationError]
}

object Validator {

  def apply[T]: Validator[T] = (_: T) => Nil

  implicit def derived[T](implicit dv: Lazy[DerivedValidator[T]]): Validator[T] =
    dv.value.validator

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
  }

  private def rule[T](pred: T => Boolean, whenInvalid: String): Validator[T] =
    (obj: T) => Try(pred(obj)) match {
      case Success(true) => Nil
      case Success(false) => List(ValidationError(whenInvalid))
      case Failure(why) => List(ValidationError(why.getMessage))
    }

  private def ruleVC[T, V](pred: V => Boolean, whenInvalid: String)
                          (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
    (obj: T) => rule[V](pred, whenInvalid)
      .validate(gen.to(obj).head)

  private def ruleField[T, R <: HList, U]
    (field: Witness, pred: U => Boolean, whenInvalid: String)
    (implicit ev: field.T <:< Symbol,
     gen: LabelledGeneric.Aux[T, R],
     sel: Selector.Aux[R, field.T, U])
  : Validator[T] =
    (obj: T) => rule[U](pred, whenInvalid)
      .validate(sel(gen.to(obj)))
      .map(field.value.name :: _)
}
