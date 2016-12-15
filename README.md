# Octopus

[![Build Status](https://travis-ci.org/krzemin/octopus.svg?branch=master)](https://travis-ci.org/krzemin/octopus)
[![codecov.io](http://codecov.io/github/krzemin/octopus/coverage.svg?branch=master)](http://codecov.io/github/krzemin/octopus?branch=master)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Octopus is a Scala library for boilerplate-free validation.

It defines `Validator[T]` type-class, provide composable DSL for
defining validation rules for user-defined type and can automatically
derive validators for case classes, tuples, sealed hierarchies and various
stadard Scala types by composing other defined or derived validators.

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
// Usually you want to put them into companion objects
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

### Getting started

Octopus is currently available for Scala 2.12.

To get started with SBT, add following line to your `build.sbt`:

```scala
libraryDependencies += "com.github.krzemin" %% "octopus" % "0.1.0"
```

## Usage

See [USAGE.md](USAGE.md)

## Design details

See [DESIGN.md](DESIGN.md)

## TODO

Things to implement/consider:

* [ ] abstraction over error message types
* [ ] asynchronous validations
* [ ] write about usage
* [ ] scala.js port
* [ ] write about design details

## FAQ

#### How it's different that Cats/Scalaz validation data types?

They main differece between Octopus and Cats/Scalaz validation types is an approach
to composability of validations.

Cats/Scalaz validations are kind of disjunction types that hold successfully validated
value or some validation error(s). They can be composed usually with simple combinators
or applicative builder syntax. When having lot of case classes with many fields and you
want to compose validators of their fields, you have to do this manually which results
with rather lot amount of boilerplate code.

Octopus approach is a bit different. The `Validator[T]` type-class holds a function
that will perform validation at some point of time. There are actually two levels of
composability: 

* validation rules for single type - can be composed using provided DSL,
* field validators that composes and create case class validator - that is achieved
  automatically by using type-class derivation mechanism; still you can override validation
  rules for certain types in local contexts.

#### What about asynchronous validations?

When defining validators usually we use predicate functions `T => Boolean` to decide
satisfiability of certain rule. This work work well as long as we can answer this
question only having access to value being validated.

But sometimes this restriction is too limiting to decide a validation - in some cases
it's necessary to query some external data source (like database). Thus, we would
want to have asynchronous predicates like `T => Future[Boolean]`.

Currently Octopus doesn't support asynchronous validations. You can always deal with
them in another layer of your application.

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
