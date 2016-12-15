package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Lazy, Witness}
import shapeless.ops.record.Selector

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

trait Validator[T] {

  def validate(obj: T): List[ValidationError]
}

object Validator {

  def apply[T]: Validator[T] = (_: T) => Nil

  def invalid[T](error: String): Validator[T] = (_: T) => List(ValidationError(error))

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

  private def rule[T](pred: T => Boolean, whenInvalid: String): Validator[T] =
    (obj: T) => if(pred(obj)) Nil else List(ValidationError(whenInvalid))

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
      .map(FieldLabel(field.value) :: _)

  private def ruleCatchOnly[T, E <: Throwable : ClassTag](pred: T => Boolean, whenInvalid: String, whenCaught: E => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
        List(ValidationError(whenCaught(ex.asInstanceOf[E])))
    }

  private def ruleCatchNonFatal[T](pred: T => Boolean, whenInvalid: String, whenCaught: Throwable => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case NonFatal(ex) =>
        List(ValidationError(whenCaught(ex)))
    }

  private def ruleTry[T](pred: T => Try[Boolean], whenInvalid: String, whenFailure: Throwable => String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Success(true) => Nil
      case Success(false) => List(ValidationError(whenInvalid))
      case Failure(why) => List(ValidationError(whenFailure(why)))
    }

  private def ruleEither[T](pred: T => Either[String, Boolean], whenInvalid: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Right(true) => Nil
      case Right(false) => List(ValidationError(whenInvalid))
      case Left(why) => List(ValidationError(why))
    }

  private def ruleOption[T](pred: T => Option[Boolean], whenInvalid: String, whenNone: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Some(true) => Nil
      case Some(false) => List(ValidationError(whenInvalid))
      case None => List(ValidationError(whenNone))
    }
}
