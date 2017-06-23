package octopus

private[octopus] object DslMacros {

  def ruleFieldSelector[T: c.WeakTypeTag, F: c.WeakTypeTag](c: scala.reflect.macros.whitebox.Context)
                                                           (selector: c.Expr[T => F], pred: c.Expr[F => Boolean], whenInvalid: c.Expr[String]): c.Expr[Validator[T]] = {
    import c.universe._
    selector.tree match {
      case q"($_) => $_.${fieldName: Name}" =>
        val sym = Symbol(fieldName.decodedName.toString)
        c.Expr[Validator[T]] {
          q"{${c.prefix}}.compose(Validator.ruleField($sym, $pred, $whenInvalid))"
        }
      case _ =>
        c.abort(c.enclosingPosition, "Invalid selector!")
    }
  }
}

