package octopus.example.domain

import octopus.dsl._

sealed trait Shape

case class Circle(radius: Double) extends Shape

object Circle {

  val Err_RadiusMustBePositive = "radius must be greater than 0"

  implicit val validator = Validator[Circle]
    .rule(_.radius > 0, Err_RadiusMustBePositive)
}

case class Rectangle(width: Double, height: Double) extends Shape

object Rectangle {

  val Err_WidthMustBePositive = "width must be greater than 0"
  val Err_HeightMustBePositive = "height must be greater than 0"

  implicit val validator = Validator[Rectangle]
    .rule(_.width > 0, Err_WidthMustBePositive)
    .rule(_.height > 0, Err_HeightMustBePositive)
}