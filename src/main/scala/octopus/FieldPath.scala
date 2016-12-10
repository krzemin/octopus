package octopus

sealed trait PathElement extends Any {
  def asString: String
}

case class FieldLabel(label: Symbol) extends AnyVal with PathElement {
  def asString: String = label.name
}

case class CollectionIndex(index: Int) extends AnyVal with PathElement {
  def asString: String = s"[$index]"
}

case class MapKey(key: String) extends AnyVal with PathElement {
  def asString: String = s"[$key]"
}


case class FieldPath(parts: List[PathElement]) extends AnyVal {

  def ::(element: PathElement): FieldPath =
    copy(element :: parts)

  def asString: String = {
    if(parts.isEmpty) {
      ""
    } else {
      parts.tail.foldLeft(parts.head.asString) {
        case (buff, element: FieldLabel) =>
          s"$buff.${element.asString}"
        case (buff, element) =>
          buff + element.asString
      }
    }
  }
}

object FieldPath {

  val empty = FieldPath(Nil)
}
