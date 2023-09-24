# Redacted

> Prevents leaking sensitive fields defined inside `case class`

In Scala, `case class`(es) omnipresent: they are the building blocks for complex business domain models, due to how
easily they can be defined and instantiated; on top of that, the Scala compiler provides a convenient `toString` method
for us that will print in console/log their content, for example:

```scala 3
case class UserPreferences(useDarkTheme: Boolean, maxHistoryItems: Int)

val id = 123
val p = store.getUserPreferencesByID(123)
log.info(s"user preferences for user $id are $p")
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

you probably don't want to leak by mistake an `apiKey` (for security reasons) or an `email` (for PII/GDPR reasons).

Sure, you can get creative and define middleware layers/utility methods and so on to circumvent the issue, but wouldn't
it be better if you were simply to say "when I dump **the whole object**, I don't want this field to be printed out" ?

`@redacted` to the rescue!

## Usage

in your `project/build.sbt` file, add the following line

```scala 3
addCompilerPlugin("io.github.polentino" % "redacted" % "0.0.1")
```

and then, in your case class definitions

```scala 3
import io.github.polentino.redacted.redacted

case class HttpHeaders(userId: UUID, @redacted apiKey: String, languages: Seq[Locale], correlationId: String)

case class User(id: UUID, nickname: String, @redacted email: String)
```

That's all!

From now on, every time you'll try to dump the whole object,or invoke `toString` method, something like this will
happen:

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

will print
>$ HttpHeaders(d58b6a78-5411-4bd4-a0d3-e1ed38b579c4, ***, Seq(it_IT, en_US), corr - id - 123)  
>$ User(8b2d4570-d043-473b-a56d-fe98105ccc2b, polentino911, ***)  

but the actual content of each individual field won't be modified, i.e. this

```scala 3
println(headers.apiKey)
println(user.email)
```
will still print the real values
>$ abcdefghijklmnopqrstuvwxyz   
>$ polentino911@somemail.com

### Nested case class!

It also works with nested case classes:

```scala 3
case class Wrapper(id: String, user: User)
val wrapper = Wrapper("id-1", user) // user is the same object defined above
println(wrapper)
```

will print
> Wrapper(id-1,User(8b2d4570-d043-473b-a56d-fe98105ccc2b, polentino911, ***))

### Nested case class with upper level annotation!

It also works with nested case classes:

```scala 3
case class Wrapper(id: String, @redacted: User)
val wrapper = Wrapper("id-1", user) // user is the same object defined above
println(wrapper)
```

will print
> Wrapper(id-1,***)

## How it works

The compiler plugin will kick in after the `Pickler` phase, and will check if the `tpd.TypeDef` being currently analyzed
represents a case class, and if it has at least one field annotated with `@redacted` annotation: if that's the case, it
will start patching the existing `toString` method definition described below, otherwise it will move on.

The patching itself is pretty simple: given the existing `toString` method defined like this

```scala 3
def toString(): String = scala.runtime.ScalaRunTime._toString(this)
```

with this

```scala 3
def toString(): String = io.github.polentino.redacted.helpers.toRedactedString(this, Seq("field_A", ..., "field_X"))
```

where `"field_A", ..., "field_X"` are all the fields that were annotated with `@redacted`

## Improvements
* [ ] define the sequence of redacted fields in a private variable
    * [ ] move aforementioned variable in the case class companion object
* [ ] publish it in maven


## Credits
* Awesome pointers and ideas by Kit Langton (although it's about macros and not compiler plugins)
  * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 1](https://www.youtube.com/watch?v=h9hCm7GRbfE)
  * [Compile-Time Time! — Data Transmogrification Macro From Scratch — Part 2](https://www.youtube.com/watch?v=w7pzqHXGnf8)
* [Compiler Plugin Development in Scala 3 | Let's talk about Scala 3](https://www.youtube.com/watch?v=oqYd_Lwj2p0)
