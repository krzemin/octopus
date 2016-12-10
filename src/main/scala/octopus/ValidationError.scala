package octopus

case class ValidationError(message: String, path: FieldPath = FieldPath.empty) {

  def ::(pathElement: PathElement): ValidationError =
    copy(path = pathElement :: path)

  def toPair: (String, String) =
    path.toString -> message

  override def toString: String =
    s"$path: $message"
}
