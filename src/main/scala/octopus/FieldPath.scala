package octopus

case class FieldPath(parts: List[String]) extends AnyVal {

  def ::(label: String): FieldPath =
    copy(label :: parts)

  override def toString: String =
    parts.mkString(".")
}

object FieldPath {

  val empty = FieldPath(Nil)

  def fromString(pathStr: String): FieldPath =
    FieldPath(pathStr.split('.').toList)
}
