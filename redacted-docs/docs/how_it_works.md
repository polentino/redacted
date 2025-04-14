---
title: "How It Works"
sidebar_position: 3
---

The logic behind `@redacted` is pretty straightforward: suppose we have a case class like this

    ```scala title="User.scala"
    case class User(id: Long, nick: String, @redacted email: String, login: LocalDateTime, @redacted age: Int)
    ```

redacted compiler plugin will modify the case class definition as such

```scala title="User.scala"
case class User(id: Long, nick: String, @redacted email: String, login: LocalDateTime, @redacted age: Int) {
  override def toString: String =
    "User(" + this.id + "," + this.nick + "," + "***" + "," + this.login + "," + "***" + ")"
}
```

In other words: the content of the variables annotated with `@redacted` won't change at all, and will be still
accessible whenever you'll access them :wink:

Furthermore, redacted compiler plugin will change only case classes that contain _at least_ one field marked with
`@redacted` annotation, skipping entirely the ones that don't have it.

If you're curious about how the compiler plugin works, feel free to have a look at the [Development guide](/development)