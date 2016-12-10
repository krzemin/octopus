package octopus

sealed trait PathElement extends Any {
  def asString: String
}

case class FieldLabel(label: Symbol) extends AnyVal with PathElement {
  def asString: String = label.name
}

case class CollectionIndex(index: Int) extends AnyVal with PathElement {
  def asString: String = index.toString
}

case class MapKey(key: String) extends AnyVal with PathElement {
  def asString: String = key
}


case class FieldPath(parts: List[PathElement]) extends AnyVal {

  def ::(element: PathElement): FieldPath =
    copy(element :: parts)

  override def toString: String =
    parts.map(_.asString).mkString(".")
}

object FieldPath {

  val empty = FieldPath(Nil)
}
