package octopus

import octopus.AsyncValidator.instance
import shapeless.{::, Generic, HNil}

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object AsyncValidationRules {

  def rule[M[_]: App, T](asyncPred: T => M[Boolean], whenInvalid: String): AsyncValidator[M, T] =
    instance { (obj: T) =>
      implicitly[App[M]].map(asyncPred(obj))(if(_) Nil else List(ValidationError(whenInvalid)))
    }

  def ruleVC[M[_]: App, T, V](asyncPred: V => M[Boolean], whenInvalid: String)
                  (implicit gen: Generic.Aux[T, V :: HNil]): AsyncValidator[M, T] =
    instance { (obj: T) =>
      rule[M, V](asyncPred, whenInvalid)
        .validate(gen.to(obj).head)
    }

  def ruleCatchOnly[M[_]: App, T, E <: Throwable : ClassTag](asyncPred: T => Future[Boolean],
                                                  whenInvalid: String,
                                                  whenCaught: E => String): AsyncValidator[M, T] =
    instance { (obj: T) =>
      implicitly[App[M]].recover(
        rule(asyncPred, whenInvalid).validate(obj), {
          case ex if implicitly[ClassTag[E]].runtimeClass.isInstance(ex) =>
            List(ValidationError(whenCaught(ex.asInstanceOf[E])))
        }
      )
    }

  def ruleCatchNonFatal[M[_]: App, T](asyncPred: T => M[Boolean],
                           whenInvalid: String,
                           whenCaught: Throwable => String): AsyncValidator[M, T] =
    instance { (obj: T) =>
      implicitly[App[M]].recover(
        rule(asyncPred, whenInvalid).validate(obj), {
          case NonFatal(ex) =>
            List(ValidationError(whenCaught(ex)))
        })
    }

  def ruleEither[M[_]: App, T](asyncPred: T => M[Either[String, Boolean]],
                    whenInvalid: String): AsyncValidator[M, T] =
    instance { (obj: T) =>
      implicitly[App[M]].map(asyncPred(obj)) {
        case Right(true) => Nil
        case Right(false) => List(ValidationError(whenInvalid))
        case Left(why) => List(ValidationError(why))
      }
    }

  def ruleOption[M[_]: App, T](asyncPred: T => M[Option[Boolean]],
                    whenInvalid: String,
                    whenNone: String): AsyncValidator[M, T] =
    instance { (obj: T) =>
      implicitly[App[M]].map(asyncPred(obj)) {
        case Some(true) => Nil
        case Some(false) => List(ValidationError(whenInvalid))
        case None => List(ValidationError(whenNone))
      }
    }
}
