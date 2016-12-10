package octopus

case class ValidationError(message: String, path: FieldPath = FieldPath.empty) {

  def ::(label: String): ValidationError =
    copy(path = label :: path)

  def toPair: (String, String) =
    path.toString -> message

  override def toString: String =
    s"$path: $message"
}
