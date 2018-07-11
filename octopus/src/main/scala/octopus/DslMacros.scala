package octopus

import scala.language.higherKinds

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

  def ruleFieldSelectorAsync[M[_], T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                                (selector: c.Expr[T => F],
                                                                 pred: c.Expr[F => M[Boolean]],
                                                                 whenInvalid: c.Expr[String])(implicit M: c.WeakTypeTag[M[_]]): c.Expr[AsyncValidator[M, T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val fieldSymbol = Symbol(fieldName.decodedName.toString)
        val T = weakTypeOf[T]
        val F = weakTypeOf[F]
        val obj = TermName(c.freshName("obj"))
        c.Expr[AsyncValidator[M, T]] {
          q"""
             {${c.prefix}}.compose {
               _root_.octopus.AsyncValidator.instance[$M, $T] { ($obj: $T) =>
                 _root_.octopus.App[$M].map(
                  _root_.octopus.AsyncValidationRules
                     .rule[$M, $F]($pred, $whenInvalid)
                     .validate($selector($obj))
                  ){ errs =>
                     errs.map(_root_.octopus.FieldLabel($fieldSymbol) :: _)
                  }
               }
             }
          """
        }
      case t =>
        c.abort(c.enclosingPosition, s"Invalid selector: $t!")
    }
  }

  def ruleFieldSelectorSync[M[_], T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)
                                                               (selector: c.Expr[T => F],
                                                                pred: c.Expr[F => Boolean],
                                                                whenInvalid: c.Expr[String]): c.Expr[AsyncValidator[M, T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val fieldSymbol = Symbol(fieldName.decodedName.toString)
        val T = weakTypeOf[T]
        val F = weakTypeOf[F]
        val obj = TermName(c.freshName("obj"))
        c.Expr[AsyncValidator[M, T]] {
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
