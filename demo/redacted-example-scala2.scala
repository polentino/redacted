//> using scala 2.13.16
//> using dep io.github.polentino:::redacted:0.7.1
//> using plugin io.github.polentino:::redacted-plugin:0.7.1

import io.github.polentino.redacted._
import io.github.polentino.redacted.{redacted => obfuscated} // that works too, should you need an alias

object Demo {
  // works with value classes, as import alias, and nested within other case class
  case class Password(@redacted hash: String) extends AnyVal
  case class Email(@obfuscated value: String) extends AnyVal
  case class UserDB(id: Long, username: String, email: Email, password: Password, @redacted age: Int)

  def main(args: Array[String]): Unit = {
    val user = UserDB(0L, "berfu", Email("berfu@somemail.com"), Password("e54d6f7gy8huinjomp"), 27)

    println(s"found user: ${user}") // sensitive fields will be redacted, if you dump the instance by mistake ...
    println(s"found user: ${user.toString}") // .. or explicitly call `toString` on it

    println( // but, accessed individually, they'll return their actual content
      s"""user '${user.username}' is ${user.age} years old, and:
         |\t* email -> ${user.email.value}
         |\t* hash  -> ${user.password.hash}""".stripMargin)
  }
}
