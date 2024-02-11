package io.github.polentino.redacted

case class RedactionWithoutCompanionObj(@redacted name: String, age: Int, @redacted email: String)

object RedactionWithoutCompanionObj {
  val something = 123
}
