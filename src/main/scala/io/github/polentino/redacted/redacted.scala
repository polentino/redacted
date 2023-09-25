package io.github.polentino.redacted

import scala.annotation.StaticAnnotation

/** Annotation used to prevent leaking sensitive data when dumping a whole case class to string.
  *
  * Usage: <blockquote><pre> case class User(id: UUID, @redacted name: String, @redacted email: String) val user =
  * User(UUID.randomUUID(), "Berfu", "berfu@gmail.com")
  *
  * println(user) >$ User(5f3ab309-2784-4b78-a798-6cf44f35edfd, ***, ***) </pre></blockquote>
  */
class redacted extends StaticAnnotation
