package octopus

import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.Selector

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

object ValidationRules {

  def rule[T](pred: T => Boolean, whenInvalid: String): Validator[T] =
    (obj: T) => if (pred(obj)) Nil else List(ValidationError(whenInvalid))

  def ruleVC[T, V](pred: V => Boolean, whenInvalid: String)
                  (implicit gen: Generic.Aux[T, V :: HNil]): Validator[T] =
    (obj: T) => rule[V](pred, whenInvalid)
      .validate(gen.to(obj).head)

  def ruleCatchOnly[T, E <: Throwable : ClassTag](pred: T => Boolean,
                                                  whenInvalid: String,
                                                  whenCaught: E => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
        List(ValidationError(whenCaught(ex.asInstanceOf[E])))
    }

  def ruleCatchNonFatal[T](pred: T => Boolean,
                           whenInvalid: String,
                           whenCaught: Throwable => String): Validator[T] =
    (obj: T) => try {
      rule(pred, whenInvalid).validate(obj)
    } catch {
      case NonFatal(ex) =>
        List(ValidationError(whenCaught(ex)))
    }

  def ruleTry[T](pred: T => Try[Boolean],
                 whenInvalid: String,
                 whenFailure: Throwable => String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Success(true) => Nil
      case Success(false) => List(ValidationError(whenInvalid))
      case Failure(why) => List(ValidationError(whenFailure(why)))
    }

  def ruleEither[T](pred: T => Either[String, Boolean],
                    whenInvalid: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Right(true) => Nil
      case Right(false) => List(ValidationError(whenInvalid))
      case Left(why) => List(ValidationError(why))
    }

  def ruleOption[T](pred: T => Option[Boolean],
                    whenInvalid: String,
                    whenNone: String): Validator[T] =
    (obj: T) => pred(obj) match {
      case Some(true) => Nil
      case Some(false) => List(ValidationError(whenInvalid))
      case None => List(ValidationError(whenNone))
    }
}
