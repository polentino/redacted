package io.github.polentino.redacted

import io.github.polentino.redacted.redacted
import io.github.polentino.redacted.RedactionWithNestedCaseClass.Inner

final case class RedactionWithNestedCaseClass(id: String, @redacted inner1: Inner, inner2: Inner)

object RedactionWithNestedCaseClass {
  final case class Inner(@redacted name: String, age: Int)
}
