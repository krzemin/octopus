package octopus

trait Validator[T] {

  def validate(obj: T): List[ValidationError]
}

object Validator {

  def instance[T](f: T => List[ValidationError]): Validator[T] =
    new Validator[T] {
      def validate(obj: T): List[ValidationError] = f(obj)
    }

  def apply[T]: Validator[T] = (_: T) => Nil

  def invalid[T](error: String): Validator[T] = (_: T) => List(ValidationError(error))

  def derived[T](implicit dv: DerivedValidator[T]): Validator[T] =
    dv.v

  implicit def fromDerivedValidator[T](implicit dv: DerivedValidator[T]): Validator[T] = dv.v
}
