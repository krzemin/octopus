package octopus

import octopus.AsyncValidator.instance
import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.Selector

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object AsyncValidationRules {

  def rule[T](asyncPred: T => Future[Boolean], whenInvalid: String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      asyncPred(obj).map {
        if (_) Nil else List(ValidationError(whenInvalid))
      }(ec)
    }

  def ruleVC[T, V](asyncPred: V => Future[Boolean], whenInvalid: String)
                  (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule[V](asyncPred, whenInvalid)
        .validate(gen.to(obj).head)(ec)
    }

  def ruleField[T, R <: HList, U](field: Witness, asyncPred: U => Future[Boolean], whenInvalid: String)
                                 (implicit ev: field.T <:< Symbol,
                                  gen: LabelledGeneric.Aux[T, R],
                                  sel: Selector.Aux[R, field.T, U])
  : AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule[U](asyncPred, whenInvalid)
        .validate(sel(gen.to(obj)))(ec)
        .map(errs => errs.map(FieldLabel(field.value) :: _))(ec)
    }

  def ruleCatchOnly[T, E <: Throwable : ClassTag](asyncPred: T => Future[Boolean],
                                                  whenInvalid: String,
                                                  whenCaught: E => String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule(asyncPred, whenInvalid)
        .validate(obj)(ec)
        .recover {
          case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
            List(ValidationError(whenCaught(ex.asInstanceOf[E])))
        }(ec)
    }

  def ruleCatchNonFatal[T](asyncPred: T => Future[Boolean],
                           whenInvalid: String,
                           whenCaught: Throwable => String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule(asyncPred, whenInvalid)
        .validate(obj)(ec)
        .recover {
          case NonFatal(ex) =>
            List(ValidationError(whenCaught(ex)))
        }(ec)
    }

  def ruleEither[T](asyncPred: T => Future[Either[String, Boolean]],
                    whenInvalid: String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      asyncPred(obj).map {
        case Right(true) => Nil
        case Right(false) => List(ValidationError(whenInvalid))
        case Left(why) => List(ValidationError(why))
      }(ec)
    }

  def ruleOption[T](asyncPred: T => Future[Option[Boolean]],
                    whenInvalid: String,
                    whenNone: String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      asyncPred(obj).map {
        case Some(true) => Nil
        case Some(false) => List(ValidationError(whenInvalid))
        case None => List(ValidationError(whenNone))
      }(ec)
    }
}
