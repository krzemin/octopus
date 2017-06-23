package octopus

import scala.concurrent.Future

private[octopus] object DslMacros {

  def ruleFieldSelector[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                           (selector: c.Expr[T => F], pred: c.Expr[F => Boolean], whenInvalid: c.Expr[String]): c.Expr[Validator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val sym = Symbol(fieldName.decodedName.toString)
        c.Expr[Validator[T]] {
          q"{${c.prefix}}.compose(Validator.ruleField($sym, $pred, $whenInvalid))"
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }

  def ruleFieldSelectorAsync[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                                (selector: c.Expr[T => F], pred: c.Expr[F => Future[Boolean]], whenInvalid: c.Expr[String]): c.Expr[AsyncValidator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val sym = Symbol(fieldName.decodedName.toString)
        c.Expr[AsyncValidator[T]] {
          q"{${c.prefix}}.compose(AsyncValidator.ruleField($sym, $pred, $whenInvalid))"
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }
}

