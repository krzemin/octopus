package example

import example.Domain._
import octopus._
import octopus.syntax._
import org.scalatest.{MustMatchers, WordSpec}

import shapeless.tag.@@
import shapeless.test.illTyped

class ValidationSpec extends WordSpec with MustMatchers {

  import Fixtures._

  "Validation" when {

    "given primitive types" should {

      "pass them all with empty validation" in {

        1.isValid mustBe true
        "abc".isValid mustBe true
        true.isValid mustBe true
        10.0.isValid mustBe true
        10.0f.isValid mustBe true
        'x'.isValid mustBe true
        3.toByte.isValid mustBe true
        66.toShort.isValid mustBe true
        ().isValid mustBe true
      }
    }

    "given simple value classes" should {

      "validate UserId" in {

        userId_Valid.isValid mustBe true

        userId_Invalid.validate mustBe List(
          ValidationError("must be positive number")
        )
      }

      "validate Email" in {

        email_Valid.isValid mustBe true

        email_Invalid1.validate mustBe List(
          ValidationError("must not be empty"),
          ValidationError("must contain @"),
          ValidationError("must contain . after @")
        )

        email_Invalid2.validate mustBe List(
          ValidationError("must contain @"),
          ValidationError("must contain . after @")
        )

        email_Invalid3.validate mustBe List(
          ValidationError("must contain . after @")
        )

        email_Invalid4.validate mustBe List(
          ValidationError("must contain @")
        )
      }

      "validate PostalCode" in {

        postalCode_Valid.isValid mustBe true

        postalCode_Invalid1.validate mustBe List(
          ValidationError("must be of length 5"),
          ValidationError("must contain only digits")
        )

        postalCode_Invalid2.validate mustBe List(
          ValidationError("must be of length 5")
        )
      }
    }

    "given case classes with few fields" should {

      "validate Address" in {

        address_Valid.isValid mustBe true

        address_Invalid1.validateAsFieldErrMapping mustBe List(
          "postalCode" -> "must be of length 5",
          "postalCode" -> "must contain only digits",
          "city" -> "must not be empty",
          "street" -> "must not be empty"
        )
      }

      "validate User" in {

        user_Valid.isValid mustBe true

        user_Invalid1.validateAsFieldErrMapping mustBe List(
          "id" -> "must be positive number",
          "email" -> "must contain @",
          "email" -> "must contain . after @",
          "address.postalCode" -> "must be of length 5",
          "address.postalCode" -> "must contain only digits",
          "address.city" -> "must not be empty",
          "address.street" -> "must not be empty"
        )

        user_Invalid2.validateAsFieldErrMapping mustBe List(
          "id" -> "must be positive number"
        )
      }
    }

    "given Option" should {

      "always pass when empty" in {

        val emptyEmail: Option[Email] = None
        emptyEmail.isValid mustBe true
      }

      "apply validation rules of inner object when not empty" in {

        val some_email_Valid: Option[Email] = Some(email_Valid)
        val some_email_Invalid4: Option[Email] = Some(email_Invalid4)

        some_email_Valid.isValid mustBe true

        some_email_Invalid4.validate mustBe List(
          ValidationError("must contain @")
        )
      }
    }

    "given Traversable" should {

      val userIds_Valid = Seq(userId_Valid, userId_Valid)
      val userIds_Invalid = userIds_Valid ++ Seq(userId_Invalid, userId_Invalid, userId_Valid)

      "validate all elements of Seq" in {

        userIds_Valid.isValid mustBe true

        userIds_Invalid.validateAsFieldErrMapping mustBe List(
          "2" -> "must be positive number",
          "3" -> "must be positive number"
        )
      }

      "validate all elements of List" in {

        userIds_Valid.toList.isValid mustBe true

        userIds_Invalid.toList.validateAsFieldErrMapping mustBe List(
          "2" -> "must be positive number",
          "3" -> "must be positive number"
        )
      }

      "validate all elements of Array" in {

        userIds_Valid.toArray.isValid mustBe true

        userIds_Invalid.toArray.validateAsFieldErrMapping mustBe List(
          "2" -> "must be positive number",
          "3" -> "must be positive number"
        )
      }

      "validate all elements of Set" in {

        userIds_Valid.toSet.isValid mustBe true

        Set(email_Valid, email_Invalid2, email_Invalid3).validateAsFieldErrMapping mustBe List(
          "1" -> "must contain @",
          "1" -> "must contain . after @",
          "2" -> "must contain . after @"
        )
      }
    }

    "given Map" should {

      "validate all values prefixing potential errors with key.toString" in {

        Map(20 -> userId_Valid, 30 -> userId_Valid).isValid mustBe true

        Map(30 -> email_Invalid2, 20 -> email_Valid, 40 -> email_Invalid3).validateAsFieldErrMapping mustBe List(
          "30" -> "must contain @",
          "30" -> "must contain . after @",
          "40" -> "must contain . after @"
        )
      }
    }

    "given value of sealed hierarchy" should {

      "validate according to rules" in {

        shape_circle_Valid.isValid mustBe true

        shape_circle_Invalid.validate mustBe List(
          ValidationError("radius must be greater than 0")
        )

        shape_rectangle_Valid.isValid mustBe true

        shape_rectangle_Invalid.validate mustBe List(
          ValidationError("width must be greater than 0"),
          ValidationError("height must be greater than 0")
        )
      }
    }

    "validation rule is overriden in local context" should {

      "respect overriden rule in validator derivation" in {

        implicit val postalCodeValidator: Validator[UserId] = Validator[UserId]
          .rule(_.id % 2 == 0, "must be even")

        user_Valid.validateAsFieldErrMapping mustBe List(
          "id" -> "must be even"
        )

        user_Invalid1.validateAsFieldErrMapping mustBe List(
          "email" -> "must contain @",
          "email" -> "must contain . after @",
          "address.postalCode" -> "must be of length 5",
          "address.postalCode" -> "must contain only digits",
          "address.city" -> "must not be empty",
          "address.street" -> "must not be empty"
        )

        user_Invalid2.isValid mustBe true
      }
    }
  }


  "Validator DSL" should {

    "validate as either" in {

      email_Valid.validateAsEither mustBe Right(email_Valid)

      email_Invalid4.validateAsEither mustBe Left(List(
        ValidationError("must contain @")
      ))
    }

    "validate as tagged either" in {

      trait Valid

      def doSomethingWithEmail(email: Email @@ Valid): Unit = ()

      email_Valid.validateAsTaggedEither[Valid].foreach(doSomethingWithEmail)

      illTyped("email_Valid.validateAsEither.foreach(doSomethingWithEmail)")
    }
  }

  object Fixtures {

    val userId_Valid = UserId(1)
    val userId_Invalid = UserId(0)

    val email_Valid = Email("abc@example.com")
    val email_Invalid1 = Email("")
    val email_Invalid2 = Email("abc")
    val email_Invalid3 = Email("abc@xyz")
    val email_Invalid4 = Email("abc.xyz")

    val postalCode_Valid = PostalCode("00385")
    val postalCode_Invalid1 = PostalCode("abc")
    val postalCode_Invalid2 = PostalCode("003850")

    val address_Valid = Address("Love Street", postalCode_Valid, "Los Angeles")
    val address_Invalid1 = Address("", postalCode_Invalid1, "")
    val address_Invalid2 = Address("Love Street", postalCode_Valid, "")

    val user_Valid = User(userId_Valid, email_Valid, address_Valid)
    val user_Invalid1 = User(userId_Invalid, email_Invalid2, address_Invalid1)
    val user_Invalid2 = User(userId_Invalid, email_Valid, address_Valid)

    val shape_circle_Valid: Shape = Circle(10.5)
    val shape_circle_Invalid: Shape = Circle(-3.1)

    val shape_rectangle_Valid: Shape = Rectangle(2.5, 4.2)
    val shape_rectangle_Invalid: Shape = Rectangle(0, -1.3)
  }

}
