![Actions Status](https://github.com/polentino/redacted/actions/workflows/ci.yml/badge.svg)
![GitHub Tag](https://img.shields.io/github/v/tag/polentino/redacted?sort=semver&label=Latest%20Tag&color=limegreen&logo=git&logoColor=limegreen)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
<!-- commented out until I figure out why they're not updated anymore
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.polentino/redacted_3.1.3?server=https%3A%2F%2Fs01.oss.sonatype.org&label=Sonatype%20-%20redacted&color=blue)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.polentino/redacted-plugin_3.1.3?server=https%3A%2F%2Fs01.oss.sonatype.org&label=Sonatype%20-%20redacted%20plugin&color=blue)
-->

# Redacted

> Prevents leaking sensitive fields defined inside `case class`.

![Simple example of @redacted usage](demo/redacted-example.gif "Sample usage")

<!-- TOC -->

* [Redacted](#redacted)
    * [Introduction](#introduction)
        * [A note on this README](#a-note-on-this-readme)
    * [Configuration](#configuration)
    * [Usage](#usage)
    * [Supported Scala Versions](#supported-scala-versions)
    * [How it works](#how-it-works)
    * [Improvements](#improvements)
    * [Credits](#credits)
    * [Adopters](#adopters)

<!-- TOC -->

## Introduction

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

### A note on this README

This Readme is being intentionally kept short; if you'd like to learn more about advanced usecases of the annotation, or
how the compiler plugin itself is structured and works, feel free to head over the project's
website https://polentino.github.io/redacted/ :) 

## Configuration

In your `project/plugins.sbt` add the following line

```scala
addSbtPlugin("io.github.polentino" % "sbt-redacted" % "1.1.0")
```

and then enable it in your specific (sub)project in `build.sbt` like so

```scala
lazy val root = (project in file("."))
  .enablePlugins(RedactedPlugin)
  .setting(
    redactedVersion := "0.9.2"
  )
```

## Usage

Once configured your project, all you have to do is the following

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

## Supported Scala Versions

`redacted` supports all Scala versions listed in the table below. However, it is advised to use the Long Term Support
ones (as listed in [the Scala website](https://www.scala-lang.org/download/all.html)).

| Scala Version | Notes |
|:-------------:|:-----:|
|     3.7.1     |   -   |
|     3.7.0     |   -   |
|     3.6.4     |   -   |
|     3.5.2     |   -   |
|     3.4.3     |   -   |
|     3.3.6     |  LTS  |
|     3.3.5     |  LTS  |
|     3.3.4     |  LTS  |
|     3.3.3     |  LTS  |
|     3.3.1     |  LTS  |
|     3.3.0     |  LTS  |
|     3.2.2     |   -   |
|     3.1.3     |   -   |
|    2.13.16    |   -   |
|    2.12.20    |   -   |

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
  "<class name>(" + this.< field not redacted > + "," + "***" + ... +")"
```

## Improvements

* [x] create Sbt plugin (https://github.com/polentino/sbt-redacted)
* [ ] add some benchmarks with jmh

## Credits

* Awesome pointers and ideas by Kit Langton (although it's about macros and not compiler plugins)
    * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 1](https://www.youtube.com/watch?v=h9hCm7GRbfE)
    * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 2](https://www.youtube.com/watch?v=w7pzqHXGnf8)
* [Compiler Plugin Development in Scala 3 | Let's talk about Scala 3](https://www.youtube.com/watch?v=oqYd_Lwj2p0)
* [act](https://github.com/nektos/act)

---

## Adopters

Here below you'll find a list of companies using `redacted` in their product(s)

Want to see your company here? [Fork the repo and submit a PR](https://github.com/polentino/redacted/fork)!

* [InvestSuite](https://www.investsuite.com) - B2B, Scalable & AI-Driven WealthTech
