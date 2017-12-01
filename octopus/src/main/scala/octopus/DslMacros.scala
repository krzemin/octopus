package octopus

import scala.concurrent.Future

private[octopus] object DslMacros {

  def ruleFieldSelector[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                           (selector: c.Expr[T => F],
                                                            pred: c.Expr[F => Boolean],
                                                            whenInvalid: c.Expr[String]): c.Expr[Validator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val fieldSymbol = Symbol(fieldName.decodedName.toString)
        val T = weakTypeOf[T]
        val F = weakTypeOf[F]
        val obj = TermName(c.freshName("obj"))
        c.Expr[Validator[T]] {
          q"""
             {${c.prefix}}.compose {
               _root_.octopus.Validator.instance[$T] { ($obj: $T) =>
                 _root_.octopus.ValidationRules
                   .rule[$F]($pred, $whenInvalid)
                   .validate($selector($obj))
                   .map(_root_.octopus.FieldLabel($fieldSymbol) :: _)
               }
             }
          """
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }

  def ruleFieldSelectorAsync[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                                (selector: c.Expr[T => F],
                                                                 pred: c.Expr[F => Future[Boolean]],
                                                                 whenInvalid: c.Expr[String]): c.Expr[AsyncValidator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val fieldSymbol = Symbol(fieldName.decodedName.toString)
        val T = weakTypeOf[T]
        val F = weakTypeOf[F]
        val obj = TermName(c.freshName("obj"))
        val ec = TermName(c.freshName("ec"))
        c.Expr[AsyncValidator[T]] {
          q"""
             {${c.prefix}}.compose {
               _root_.octopus.AsyncValidator.instance[$T] { ($obj: $T, $ec: _root_.scala.concurrent.ExecutionContext) =>
                 _root_.octopus.AsyncValidationRules
                   .rule[$F]($pred, $whenInvalid)
                   .validate($selector($obj))($ec)
                   .map(errs => errs.map(_root_.octopus.FieldLabel($fieldSymbol) :: _))($ec)
               }
             }
          """
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }

  def ruleFieldSelectorSync[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                               (selector: c.Expr[T => F],
                                                                pred: c.Expr[F => Boolean],
                                                                whenInvalid: c.Expr[String]): c.Expr[AsyncValidator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val fieldSymbol = Symbol(fieldName.decodedName.toString)
        val T = weakTypeOf[T]
        val F = weakTypeOf[F]
        val obj = TermName(c.freshName("obj"))
        c.Expr[AsyncValidator[T]] {
          q"""
             {${c.prefix}}.compose {
               _root_.octopus.Validator.instance[$T] { ($obj: $T) =>
                 _root_.octopus.ValidationRules
                   .rule[$F]($pred, $whenInvalid)
                   .validate($selector($obj))
                   .map(_root_.octopus.FieldLabel($fieldSymbol) :: _)
               }
             }
          """
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }
}
