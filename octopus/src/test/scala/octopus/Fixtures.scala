package octopus

import octopus.example.domain._

trait Fixtures {

  val userId_Valid = UserId(1)
  val userId_Invalid = UserId(0)

  val email_Valid = Email("x@y.com")
  val email_Valid_Long = Email("abc@example.com")
  val email_Invalid1 = Email("")
  val email_Invalid2 = Email("abc")
  val email_Invalid3 = Email("abc@xyz")
  val email_Invalid4 = Email("abc.xyz")

  val postalCode_Valid = PostalCode("33333")
  val postalCode_Invalid1 = PostalCode("abc")
  val postalCode_Invalid2 = PostalCode("003850")

  val address_Valid = Address("Love Street", postalCode_Valid, "Los Angeles")
  val address_Invalid1 = Address("", postalCode_Invalid1, "")
  val address_Invalid2 = Address("Love Street", postalCode_Valid, "")

  val name_Valid = Name("John")

  val user_Valid = User(userId_Valid, email_Valid, address_Valid, name_Valid)
  val user_Valid2 = User(userId_Valid, email_Valid_Long, address_Valid, name_Valid)
  val user_Invalid1 = User(userId_Invalid, email_Invalid2, address_Invalid1, name_Valid)
  val user_Invalid2 = User(userId_Invalid, email_Valid, address_Valid, name_Valid)
  val user_Invalid3 = User(userId_Valid, email_Invalid1, address_Valid, name_Valid)

  val shape_circle_Valid: Shape = Circle(10.5)
  val shape_circle_Invalid: Shape = Circle(-3.1)

  val shape_rectangle_Valid: Shape = Rectangle(2.5, 4.2)
  val shape_rectangle_Invalid: Shape = Rectangle(0, -1.3)
}

object Fixtures extends Fixtures
