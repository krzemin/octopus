package octopus

trait Validator[T] {

  def validate(obj: T): List[ValidationError]
}

object Validator extends ValidatorDerivation {

  def instance[T](f: T => List[ValidationError]): Validator[T] =
    new Validator[T] {
      def validate(obj: T): List[ValidationError] = f(obj)
    }

  def apply[T]: Validator[T] = (_: T) => Nil

  def invalid[T](error: String): Validator[T] = (_: T) => List(ValidationError(error))

  def derived[T](implicit dv: Validator[T]): Validator[T] =
    dv
}
