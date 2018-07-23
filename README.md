# Octopus

[![Build Status](https://travis-ci.org/krzemin/octopus.svg?branch=master)](https://travis-ci.org/krzemin/octopus)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.krzemin/octopus_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Coctopus)
[![codecov.io](http://codecov.io/github/krzemin/octopus/coverage.svg?branch=master)](http://codecov.io/github/krzemin/octopus?branch=master)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Octopus is a Scala library for boilerplate-free validation.

It defines `Validator[T]` type-class, provide composable DSL for
defining validation rules for user-defined type and can automatically
derive validators for case classes, tuples, sealed hierarchies and various
standard Scala types by composing other defined or derived validators.

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

import octopus.dsl._

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
  .rule(_.city, (_: String).nonEmpty, "must not be empty")
  .rule(_.street, (_: String).nonEmpty, "must not be empty")
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

Octopus is currently available for Scala 2.11, 2.12 and Scala.js.

To get started with SBT, add following line to your `build.sbt`:

```scala
libraryDependencies += "com.github.krzemin" %% "octopus" % "0.3.3"
```

Or if you are using Scala.js:

```scala
libraryDependencies += "com.github.krzemin" %%% "octopus" % "0.3.3"
```

### Integration with Cats / Scalaz

There are available additional modules that simplify integration with
Cats and Scalaz validation types.

#### Cats

If you want to integrate with Cats, simply add following line to `build.sbt`:

```scala
libraryDependencies += "com.github.krzemin" %%% "octopus-cats" % "0.3.3"
```

Having this dependency on classpath, you can use 

```scala
import octopus.syntax._
import octopus.cats._

user1.validate.toValidatedNel // : ValidatedNel[octopus.ValidationError, User] = Valid(user1)

user2.validate.toValidatedNel // : ValidatedNel[octopus.ValidationError, User] = Invalid(NonEmptyList(...))
```

See [integration test suite](https://github.com/krzemin/octopus/blob/master/octopusCats/src/test/scala/octopus/cats/CatsIntegrationSpec.scala)
for more information.


#### Scalaz

Alternatively, if you want similar integration with Scalaz, add following line to `build.sbt`:

```scala
libraryDependencies += "com.github.krzemin" %%% "octopus-scalaz" % "0.3.3"
```

See [integration test suite](https://github.com/krzemin/octopus/blob/master/octopusScalaz/src/test/scala/octopus/scalaz/ScalazIntegrationSpec.scala)
for reference.


### Asynchronous validators

Sometimes validation rules are more complex in sense that they can't be decided
locally by only looking at object value, but they require some external context
like querying service or database. Therefore, Octopus has support for asynchronous
predicates, that instead of `T => Boolean`, are defined in terms of `T => Future[Boolean]`.
The same as with normal validation predicates, full derivation is also supported for
asynchronous validators. Look at the example below to get better insight:

```scala
trait EmailService {
  def isEmailTaken(email: String): Future[Boolean]
  def doesDomainExists(email: String): Future[Boolean]
}

class AsyncValidators(emailService: EmailService) {

  implicit val emailAsyncValidator: AsyncValidator[Email] =
    Validator
      .derived[Email] // (1)
      .async.ruleVC(emailService.isEmailTaken, "email is already taken by someone else") // (2)
      .async.rule(_.address, emailService.doesDomainExists, "domain does not exists") // (3)
}

val asyncValidators = new AsyncValidators(...)

import asyncValidators._ // (4)

Email("abc@xyz.qux").isValidAsync // Success(false): Future[Boolean]
Email("abc@xyzqux").validateAsync
  .map(_.toFieldErrMapping)
  // Success(List(("", "must contain . after @"), ("", "domain does not exists"))): Future[List[(String, String)]


val user1 = User(
  UserId(1),
  Email("taken@example.com"),
  Address("Love Street", PostalCode("12345"), "Los Angeles")
)

user1.validateAsync
  .map(_.toFieldErrMapping) // Success(List("email", "email is already taken by someone else")): Future[List[(String, String)]
```

Comments:

* (1) we are requesting to derive usual validator for `Email` type
* (2) by prepending rule with `async` keyword we can define validator rule with
  asynchronous predicate that lifts our validator to `AsyncValidator[Email]`
* (3) we are adding next asynchronous validation rule
* (4) we are importing instances for asynchronous validators into current scope
  so that later we can use `.isValidAsync`/`.validateAsync` extension methods.


#### Using other monad

When working with asynchronous validators DSL, by default you define rules and
obtain results in scala Future. However you're not particularly tied to it.

We provide bridge instances for internal `AppError` (which resembles applicative
functor with error handling capabilities) for cats `ApplicativeError` and scalaz
`MonadError`. Any wrapper type `M[_]` for which you already have those instances,
will work. Otherwise, you need to define your own instance for `AppError` and make
sure it's in implicit scope when using rule dsl.

Example integration with `cats.effect.IO`:

```scala
  import octopus.async.cats.implicits._
  import cats.effect.IO

  trait EmailService {
    def isEmailTaken(email: String): IO[Boolean]
    def doesDomainExists(email: String): IO[Boolean]
  }

  implicit val emailAsyncValidator: AsyncValidatorM[IO, Email] =
    Validator
      .derived[Email]
      .asyncM[IO].ruleVC(emailService.isEmailTaken, "email is already taken by someone else") // (2)
      .async.rule(_.address, emailService.doesDomainExists, "domain does not exists") // (3)

  Email("abc@xyz.qux").isValidAsync // IO[Boolean]
```

## FAQ

#### How it's different that Cats/Scalaz validation data types?

The main difference between Octopus and Cats/Scalaz validation types is an approach
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

## License

Copyright 2016-2018 Piotr Krzemiński

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
