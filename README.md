# Octopus

Octopus is a Scala library for boilerplate-free, structural validation.

It defines `Validator[T]` type-class, provide composable DSL for
defining validation rules for user-defined type and can automatically
derive validators for 

### Example

Let's consider example business domain.

```scala
case class UserId(id: Int) extends AnyVal

case class Email(address: String) extends AnyVal

case class PostalCode(code: String) extends AnyVal

case class Address(street: String,
                   postalCode: PostalCode,
                   city: String)

case class User(id: UserId,
                email: Email,
                address: Address)
```

Let's define validation rules as implicit type class instances.

```scala
// Usually you wan't to put them into companion objects
// or group them together in a module.

import octopus._

implicit val userIdValidator: Validator[UserId] = Validator[UserId]
  .rule(_.id > 0, "must be positive number")

implicit val emailValidator: Validator[Email] = Validator[Email]
  .rule(_.address.nonEmpty, "must not be empty")
  .rule(_.address.contains("@"), "must contain @")
  .rule(_.address.split('@').last.contains("."), "must contain . after @")

implicit val potalCodeValidator: Validator[PostalCode] = Validator[PostalCode]
  .ruleVC((_: String).length == 5, "must be of length 5")
  .ruleVC((_: String).forall(_.isDigit), "must contain only digits")

implicit val addressValidator: Validator[Address] = Validator
  .derived[Address] // derives default validator for Address
  .ruleField('city, (_: String).nonEmpty, "must not be empty")
  .ruleField('street, (_: String).nonEmpty, "must not be empty")
```

Then, having validator instances imported, we can validate
our bigger case classes for free, without any additional boilerplate!

```scala
import octopus.syntax._

val user1 = User(
  UserId(1),
  Email("abc@example.com"),
  Address("Love Street", PostalCode("12345"), "Los Angeles")
)

val user2 = User(
  UserId(0),
  Email("abc@xyz"),
  Address("", PostalCode("qqqqqq"), "   ")
)

user1.isValid // : Boolean = true

user1.validate.toEither // : Either[octopus.ValidationError, User] = Right(user1)

user2.isValid // : Boolean = false

user2.validate.toFieldErrMapping
// : List[(String, String)] = List(
//     (id,must be positive number), 
//     (email,must contain . after @),
//     (address.postalCode,must be of length 5), 
//     (address.postalCode,must contain only digits),
//     (address.street,must not be empty)
// )
```

### How to use it?

Current status of the library is early proof-of-concept preview. Hope it will be released soon.

If you want to experiment, you can depend on git project in your sbt project:

```scala
lazy val root = Project("MyProject", file("."))
  .dependsOn(RootProject(uri("https://github.com/krzemin/octopus.git#master")))
```

## Usage

See [USAGE.md](USAGE.md)

## Design details

See [DESIGN.md](DESIGN.md)

## TODO

Things to implement/consider:

* [x] precisely typed field paths (own ADT instead of list of string)
* [ ] abstraction over error message types
* [ ] write about usage
* [ ] release an artifact
* [ ] scala.js port
* [ ] write about design details

## FAQ

#### How it's different that Cats/Scalaz validation type-classes?

#### What about asynchronous validations?


## License

Copyright 2016 Piotr Krzemiński

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
