package octopus

import shapeless.ops.record.Selector
import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Witness}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait AsyncValidator[T] {

  def validate(obj: T)
              (implicit ec: ExecutionContext): Future[List[ValidationError]]
}

object AsyncValidator extends AsyncValidatorDerivation {

  def instance[T](f: (T, ExecutionContext) => Future[List[ValidationError]]): AsyncValidator[T] =
    new AsyncValidator[T] {
      def validate(obj: T)(implicit ec: ExecutionContext): Future[List[ValidationError]] =
        f(obj, ec)
    }

  def lift[T](v: Validator[T]): AsyncValidator[T] =
    instance { (obj: T, _: ExecutionContext) =>
      Future.successful(v.validate(obj))
    }

  def apply[T]: AsyncValidator[T] =
    lift(Validator.apply[T])

  def invalid[T](error: String): AsyncValidator[T] =
    lift(Validator.invalid(error))

  private[octopus] def rule[T](asyncPred: T => Future[Boolean], whenInvalid: String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      asyncPred(obj).map {
        if(_) Nil else List(ValidationError(whenInvalid))
      }(ec)
    }

  private[octopus] def ruleVC[T, V](asyncPred: V => Future[Boolean], whenInvalid: String)
                                   (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule[V](asyncPred, whenInvalid)
        .validate(gen.to(obj).head)(ec)
    }

  private[octopus] def ruleField[T, R <: HList, U]
    (field: Witness, asyncPred: U => Future[Boolean], whenInvalid: String)
    (implicit ev: field.T <:< Symbol,
     gen: LabelledGeneric.Aux[T, R],
     sel: Selector.Aux[R, field.T, U])
  : AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      rule[U](asyncPred, whenInvalid)
        .validate(sel(gen.to(obj)))(ec)
        .map(errs => errs.map(FieldLabel(field.value) :: _))(ec)
    }

  private[octopus] def ruleCatchOnly[T, E <: Throwable : ClassTag](asyncPred: T => Future[Boolean],
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

  private[octopus] def ruleCatchNonFatal[T](asyncPred: T => Future[Boolean],
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

  private[octopus] def ruleEither[T](asyncPred: T => Future[Either[String, Boolean]],
                                     whenInvalid: String): AsyncValidator[T] =
    instance { (obj: T, ec: ExecutionContext) =>
      asyncPred(obj).map {
        case Right(true) => Nil
        case Right(false) => List(ValidationError(whenInvalid))
        case Left(why) => List(ValidationError(why))
      }(ec)
    }

  private[octopus] def ruleOption[T](asyncPred: T => Future[Option[Boolean]],
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
