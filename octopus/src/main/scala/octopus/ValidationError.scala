package octopus

case class ValidationError(message: String, path: FieldPath = FieldPath.empty) extends Serializable {

  def ::(pathElement: PathElement): ValidationError =
    copy(path = pathElement :: path)

  def toPair: (String, String) =
    path.asString -> message

  def asString: String =
    s"${path.asString}: $message"
}
