package io.github.polentino.redacted

case class RedactionWithCompanionObj(@redacted name: String, age: Int, @redacted email: String)

object RedactionWithCompanionObj {
  val something = 123
}
