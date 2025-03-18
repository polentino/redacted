![Actions Status](https://github.com/polentino/redacted/actions/workflows/ci.yml/badge.svg)
![GitHub Tag](https://img.shields.io/github/v/tag/polentino/redacted?sort=semver&label=Latest%20Tag&color=limegreen)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.polentino/redacted_3.1.3?server=https%3A%2F%2Fs01.oss.sonatype.org&label=Sonatype%20-%20redacted&color=blue)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.polentino/redacted-plugin_3.1.3?server=https%3A%2F%2Fs01.oss.sonatype.org&label=Sonatype%20-%20redacted%20plugin&color=blue)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

# Redacted

> Prevents leaking sensitive fields defined inside `case class`.

![Simple example of @redacted usage](demo/redacted-example.gif "Sample usage")

<!-- TOC -->
* [Redacted](#redacted)
  * [Usage](#usage)
    * [Nested case class](#nested-case-class)
    * [Nested case class with upper level annotation](#nested-case-class-with-upper-level-annotation)
    * [Value case classes](#value-case-classes)
    * [Note on curried case classes](#note-on-curried-case-classes)
  * [Supported Scala Versions](#supported-scala-versions)
  * [How it works](#how-it-works)
  * [Improvements](#improvements)
  * [Credits](#credits)
<!-- TOC -->

In Scala, `case class`(es) are omnipresent: they are the building blocks for complex business domain models, due to how
easily they can be defined and instantiated; on top of that, the Scala compiler provides a convenient `toString` method
for us that will pretty print in console/log their content, for example:

```scala 3
case class UserPreferences(useDarkTheme: Boolean, maxHistoryItems: Int)

val id = 123
val up = store.getUserPreferencesByID(123)
log.info(s"user preferences for user $id are $up")
```

will print

> user preferences for user 123 are UserPreferences(true, 5)

However, this becomes a double-edge sword when handling sensitive data: assume you're writing an HTTP server, and you
have a case class to pass its headers around, i.e.

```scala 3
case class HttpHeaders(userId: String, apiKey: String, languages: Seq[Locale], correlationId: String)
```

or a case class representing a user in a DB

```scala 3
case class User(id: UUID, nickname: String, email: String)
```

you probably wouldn't want to leak by mistake an `apiKey` (for security reasons) or an `email` (for PII/GDPR reasons).

Sure, you can get creative and define middleware layers/utility methods and so on to circumvent the issue, but wouldn't
it be better if you were simply to say "when I dump **the whole object**, I don't want this field to be printed out" ?

`@redacted` to the rescue!

## Usage

No matter of the scala version you use (redacted is available for Scala `2.12.x`, `2.13.x` and all `3.x` LTS versions),
all you have to do is open `build.sbt` file, add the following lines

```scala 3
val redactedVersion = "x.y.z" // use latest version of the library
// resolvers += DefaultMavenRepository

libraryDependencies ++= Seq(
  "io.github.polentino" %% "redacted" % redactedVersion cross CrossVersion.full,
  compilerPlugin("io.github.polentino" %% "redacted-plugin" % redactedVersion cross CrossVersion.full)
)
```

and then, in your case class definitions

```scala 3
import io.github.polentino.redacted.redacted

case class HttpHeaders(userId: UUID, @redacted apiKey: String, languages: Seq[Locale], correlationId: String)

case class User(id: UUID, nickname: String, @redacted email: String)
```

That's all!

From now on, every time you'll try to dump the whole object,or invoke `toString` method

```scala 3
val headers: HttpHeaders = HttpHeaders(
  userId = UUID.randomUUID(),
  apiKey = "abcdefghijklmnopqrstuvwxyz",
  languages = Seq("it_IT", "en_US"),
  correlationId = "corr-id-123"
)
val user: User = User(
  id = UUID.randomUUID(),
  nickname = "polentino911",
  email = "polentino911@somemail.com"
)
println(headers)
println(user)
```

this will actually be printed
> $ HttpHeaders(d58b6a78-5411-4bd4-a0d3-e1ed38b579c4, ***, Seq(it_IT, en_US), corr-id-123)  
> $ User(8b2d4570-d043-473b-a56d-fe98105ccc2b, polentino911, ***)

But, of course, accessing the field itself will return its content, i.e.

```scala 3
println(headers.apiKey)
println(user.email)
```

will still print the real values:
> $ abcdefghijklmnopqrstuvwxyz   
> $ polentino911@somemail.com

### Nested case class

It also works with nested case classes:

```scala 3
case class Wrapper(id: String, user: User)

val wrapper = Wrapper("id-1", user) // user is the same object defined above
println(wrapper)
```

will print
> Wrapper(id-1,User(8b2d4570-d043-473b-a56d-fe98105ccc2b, polentino911, ***))

### Nested case class with upper level annotation

It also works with nested case classes:

```scala 3
case class Wrapper(id: String, @redacted user: User)

val wrapper = Wrapper("id-1", user) // user is the same object defined above
println(wrapper)
```

will print
> Wrapper(id-1,***)

### Value case classes

`@redacted` plays nicely with value case classes too, i.e.

```scala 3
case class Password(@redacted value: String) extends AnyVal

val p = Password("somepassword")
println(p)
```

will print on console

```scala 3
Password(***)
```

### Note on curried case classes

While it is possible to write something like

```scala 3
case class Curried(id: String, @redacted name: String)(@redacted email: String)
```

the `toString` method that Scala compiler generates by default will print only the parameters in the primary
constructor, meaning that

```scala 3
val c = Curried(0, "Berfu")("berfu@gmail.com")
println(c)
```

will display

```scala 3
Curried(0, Berfu)
```

Therefore, the same behavior is being kept in the customized `toString` implementation.


## Supported Scala Versions

| Scala Version |       LTS ?        |
|:-------------:|:------------------:|
|     3.6.4     |        :x:         |
|     3.5.2     |        :x:         |
|     3.4.3     |        :x:         |
|     3.3.5     | :white_check_mark: |
|     3.3.4     | :white_check_mark: |
|     3.3.3     | :white_check_mark: |
|     3.3.1     | :white_check_mark: |
|     3.3.0     | :white_check_mark: |
|     3.2.2     |        :x:         |
|     3.1.3     |        :x:         |
|    2.13.16    |        :x:         |
|    2.12.20    |        :x:         |


## How it works

Given a case class with at least one field annotated with `@redacted`, i.e.

```scala 3
final case class User(id: UUID, @redacted name: String)
```

the compiler plugin will replace the default implementation of its `toString` method with this

```scala 3
final case class User(id: UUID, @redacted name: String) {
  def toString(): String = "User(" + this.id + ",***" + ")"
}
```

The way it's done is the following:

The compiler plugin will inspect each type definition and check whether the class being analysed is a `case class`, and
if it has at least one of its fields annotated with `@redacted` ; if that's the case, it will then proceed to rewrite
the default `toString` implementation by selectively returning either the `***` string, or the value of the field,
depending on the presence (or not) of `@redacted`, resulting in an implementation that looks like so:

```scala 3
def toString(): String =
  "<class name>(" + this.< field not redacted > + "," + "***" +
...+")"
```

## Improvements

* [ ] create Sbt plugin
* [ ] add some benchmarks with jmh

## Credits

* Awesome pointers and ideas by Kit Langton (although it's about macros and not compiler plugins)
    * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 1](https://www.youtube.com/watch?v=h9hCm7GRbfE)
    * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 2](https://www.youtube.com/watch?v=w7pzqHXGnf8)
* [Compiler Plugin Development in Scala 3 | Let's talk about Scala 3](https://www.youtube.com/watch?v=oqYd_Lwj2p0)
* [act](https://github.com/nektos/act)
