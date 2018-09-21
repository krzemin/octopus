package octopus

import octopus.AsyncValidatorM.instance
import shapeless.{::, Generic, HNil}

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object AsyncValidationRules extends Serializable {

  def rule[M[_]: AppError, T](asyncPred: T => M[Boolean], whenInvalid: String): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      AppError[M].map(asyncPred(obj))(if(_) Nil else List(ValidationError(whenInvalid)))
    }

  def ruleVC[M[_]: AppError, T, V](asyncPred: V => M[Boolean], whenInvalid: String)
                  (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      rule[M, V](asyncPred, whenInvalid)
        .validate(gen.to(obj).head)
    }

  def ruleCatchOnly[M[_]: AppError, T, E <: Throwable : ClassTag](asyncPred: T => M[Boolean],
                                                  whenInvalid: String,
                                                  whenCaught: E => String): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      AppError[M].recover(
        rule(asyncPred, whenInvalid).validate(obj), {
          case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
            List(ValidationError(whenCaught(ex.asInstanceOf[E])))
        }
      )
    }

  def ruleCatchNonFatal[M[_]: AppError, T](asyncPred: T => M[Boolean],
                           whenInvalid: String,
                           whenCaught: Throwable => String): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      AppError[M].recover(
        rule(asyncPred, whenInvalid).validate(obj), {
          case NonFatal(ex) =>
            List(ValidationError(whenCaught(ex)))
        })
    }

  def ruleEither[M[_]: AppError, T](asyncPred: T => M[Either[String, Boolean]],
                    whenInvalid: String): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      AppError[M].map(asyncPred(obj)) {
        case Right(true) => Nil
        case Right(false) => List(ValidationError(whenInvalid))
        case Left(why) => List(ValidationError(why))
      }
    }

  def ruleOption[M[_]: AppError, T](asyncPred: T => M[Option[Boolean]],
                    whenInvalid: String,
                    whenNone: String): AsyncValidatorM[M, T] =
    instance { (obj: T) =>
      AppError[M].map(asyncPred(obj)) {
        case Some(true) => Nil
        case Some(false) => List(ValidationError(whenInvalid))
        case None => List(ValidationError(whenNone))
      }
    }
}
