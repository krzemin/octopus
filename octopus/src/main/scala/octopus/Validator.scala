package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.Selector

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

trait Validator[T] {

  def validate(obj: T): List[ValidationError]
}

object Validator extends ValidatorDerivation {

  def apply[T]: Validator[T] = (_: T) => Nil

  def invalid[T](error: String): Validator[T] = (_: T) => List(ValidationError(error))

  def derived[T](implicit dv: Validator[T]): Validator[T] =
    dv

  private[octopus] def rule[T](pred: T => Boolean, whenInvalid: String): Validator[T] =
    (obj: T) => if(pred(obj)) Nil else List(ValidationError(whenInvalid))

  private[octopus] def ruleVC[T, V](pred: V => Boolean, whenInvalid: String)
                          (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
    (obj: T) => rule[V](pred, whenInvalid)
      .validate(gen.to(obj).head)

  private[octopus] def ruleField[T, R <: HList, U]
    (field: Witness, pred: U => Boolean, whenInvalid: String)
    (implicit ev: field.T <:< Symbol,
     gen: LabelledGeneric.Aux[T, R],
     sel: Selector.Aux[R, field.T, U])
  : Validator[T] =
    (obj: T) => rule[U](pred, whenInvalid)
      .validate(sel(gen.to(obj)))
      .map(FieldLabel(field.value) :: _)

  private[octopus] def ruleCatchOnly[T, E <: Throwable : ClassTag](pred: T => Boolean,
                                                                   whenInvalid: String,
                                                                   whenCaught: E => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
        List(ValidationError(whenCaught(ex.asInstanceOf[E])))
    }

  private[octopus] def ruleCatchNonFatal[T](pred: T => Boolean,
                                            whenInvalid: String,
                                            whenCaught: Throwable => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case NonFatal(ex) =>
        List(ValidationError(whenCaught(ex)))
    }

  private[octopus] def ruleTry[T](pred: T => Try[Boolean],
                                  whenInvalid: String,
                                  whenFailure: Throwable => String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Success(true) => Nil
      case Success(false) => List(ValidationError(whenInvalid))
      case Failure(why) => List(ValidationError(whenFailure(why)))
    }

  private[octopus] def ruleEither[T](pred: T => Either[String, Boolean],
                                     whenInvalid: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Right(true) => Nil
      case Right(false) => List(ValidationError(whenInvalid))
      case Left(why) => List(ValidationError(why))
    }

  private[octopus] def ruleOption[T](pred: T => Option[Boolean],
                                     whenInvalid: String,
                                     whenNone: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Some(true) => Nil
      case Some(false) => List(ValidationError(whenInvalid))
      case None => List(ValidationError(whenNone))
    }
}
