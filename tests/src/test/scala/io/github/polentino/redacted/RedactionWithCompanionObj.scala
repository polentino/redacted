package io.github.polentino.redacted

import io.github.polentino.redacted.redacted

case class RedactionWithCompanionObj(@redacted name: String, age: Int, @redacted email: String)

object RedactionWithCompanionObj {
  val something = 123
}
