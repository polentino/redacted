---
title: "Usage"
sidebar_position: 2
---

## Basic Usage

Once you enabled your SBT (sub)project(s) via sbt-plugin (or by manually adding the dependencies), usage of `@redacted`
is really straightforward. Assume the (purely educational) scenario where you would like to secure your own
implementation for HTTP headers, you could do something like this

```scala title="com/organization/project/HttpHeaders.scala"
import io.github.polentino.redacted._

final case class HttpHeaders(
  contentType: String,
  @redacted userID: Long,
  @redacetd authorization: String,
  acceptedLanguage: Set[Locale])
```

and now you will be able to log securely to file or console your headers, without fear of leaking by mistake the
`authorization` token or the `userID`

```scala
val httpHeaders: HttpHeaders = // a reference to the http headers of your application
logger.debug(httpHeaders)
```

which will print for example

```scala
> HttpHeaders(application/json,***,***,Set(ENGLISH))
```

But that can be a little confusing; if you like, you can also create value classes for sensitive data like passwords,
tokens, certificates and so on

```scala title="com/organization/project/secrets/package.scala"
import io.github.polentino.redacted._

package object secrets {
  final case class UserID(@redacted value: Long) extends AnyVal
  final case class Password(@redacted value: String) extends AnyVal
  final case class Token(@redacted value: String) extends AnyVal
  final case class PemCertificate(@redacted bytes: List[Byte]) extends AnyVal
  // ... other case class definitions for sensitive data
}
```

and change the definition of `HttpHeaders` like so

```scala title="com/organization/project/HttpHeaders.scala"
import com.organization.project.secrets._

final case class HttpHeaders(
  contentType: String,
  userId: UserID,
  authorization: Token,
  acceptedLanguage: Set[Locale])
```

and now, the same example shown above will print this instead

```scala
> HttpHeaders(application/json,UserID(***),Token(***),Set(ENGLISH))
```

Looks nicer, right?

### Quick recap

With the example above, you saw already a few good use cases of how `@redacted` works:

* it works with case classes
* it works with value classes as well
* it works with one or more annotated fields

However, there's more to it: keep reading!

## Will it work if ...

### I nest the case class into another class/object ?

Yes! This is a totally valid, alternate way to write our educational `HttpHeaders` class

```scala title="com/organization/project/HttpHeaders.scala"
import HttpHeaders._

final case class HttpHeaders(
  contentType: String,
  userId: UserID,
  authorization: Token,
  acceptedLanguage: Set[Locale])

object HttpHeaders {
  final case class UserID(@redacted value: Long) extends AnyVal
  final case class Password(@redacted value: String) extends AnyVal
}
```

### I use the FQDN of the annotation ?

Yes! Once again, here's an alternate way to write our educational `secrets` package object

```scala title="com/organization/project/secrets/package.scala"
import io.github.polentino.redacted._

package object secrets {
  final case class UserID(@io.github.polentino.redacted.redacted value: Long) extends AnyVal
  final case class Password(@io.github.polentino.redacted.redacted value: String) extends AnyVal
  final case class Token(@io.github.polentino.redacted.redacted value: String) extends AnyVal
  final case class PemCertificate(@io.github.polentino.redacted.redacted bytes: List[Byte]) extends AnyVal
  // ... other case class definitions for sensitive data
}
```

### I define an alias for `@redacted` ?

Yes! Here's another alternate way to define the `secrets` package object

```scala title="com/organization/project/secrets/package.scala"
import io.github.polentino.redacted.{redacted => obfuscated}

package object secrets {
  final case class UserID(@obfuscated value: Long) extends AnyVal
  final case class Password(@obfuscated value: String) extends AnyVal
  final case class Token(@obfuscated value: String) extends AnyVal
  final case class PemCertificate(@obfuscated bytes: List[Byte]) extends AnyVal
  // ... other case class definitions for sensitive data
}
```

## What happens if ...

### the `hashCode` of a redacted case class is used ?

`hashCode` is essential for hash-based collections such as `HashMap`, `HashSet` and so on, therefore its correctness is
of utmost importance.

Since `@redacted` doesn't change the values of the annotated fields, but only the way `toString` is implemented,
`hashCode` remains unaltered.

:::tip
And, just in case, there is also
a [specific test](https://github.com/polentino/redacted/blob/main/tests/src/test/scala/io/github/polentino/redacted/RedactedSpec.scala#L218C1-L219C1)
that ensures the `hashCode` of two similar (in fields definition) case classes, one redacted, the other not, yield the
same `hashCode` result.
:::

### I use it in a curried parameter ?

It won't work.

While the following definition is syntactically valid Scala code

```scala title="CurriedParameter.scala"
final case class CurriedParameter(id: Long)(@redacted email: String)
```

the field `email` won't contribute at all to the `toString` result, therefore using the annotation is irrelevant.

### I use it in a method of the case class ?

It won't work, and the plugin won't confuse method parameters with case class fields, if that's what you're asking.

For example, this won't redact `email` at all

```scala title="ConfusingParameter.scala"
final case class CurriedParameter(id: Long, email: String) {
  // WRONG!
  def toUpper(@redacted email: String): String = ...
}
```

while this will, although it won't affect the `email` parameter of the `toUpper` method

```scala title="ConfusingParameter.scala"
final case class CurriedParameter(id: Long,@redacted email: String) {
  def toUpper(email: String): String = ...
}
```

### I define a fake `redacted` annotation ?

This won't work either. If you try to define a fake `redacted` annotation

```scala title="com/organization/project/redacted.scala"
import scala.annotation.StaticAnnotation

class redacted extends StaticAnnotation
```

and then try to use it in our educational `HttpHeaders` case class

```scala title="com/organization/project/HttpHeaders.scala"
import com.organization.project.redacted

final case class HttpHeaders(
  contentType: String,
  @redacted userID: Long,
  @redacetd authorization: String,
  acceptedLanguage: Set[Locale])
```

those annotations **won't have any effect**!