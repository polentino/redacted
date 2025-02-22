//> using scala 2.13.16
//> using dep io.github.polentino:::redacted:0.6.1
//> using plugin io.github.polentino:::redacted-plugin:0.6.1

import io.github.polentino.redacted._

object Demo {
  // works with value classes
  case class Password(@redacted hash: String) extends AnyVal

  // works with nested redacted classes too
  case class UserDB(id: Long, username: String, password: Password, @redacted age: Int)

  def main(args: Array[String]): Unit = {
    val user = UserDB(0L, "admin", Password("e54d6f7gy8huinjomp"), 25)

    // sensitive fields will be redacted
    println(s"found user: ${user}")

    // but accessed individually, they'll return their actual content
    println(s"user '${user.username}' is ${user.age} years old, and has password hash: ${user.password.hash}")
  }
}
