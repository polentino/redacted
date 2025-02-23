package io.github.polentino.redacted

import scala.annotation.StaticAnnotation

/** Annotation used to prevent leaking sensitive data when dumping a whole case class to string.
  *
  * Example:
  * {{{
  * // definition
  * case class User(id: UUID, @redacted name: String, @redacted email: String)
  * // use as you would normally do
  * val user = User(UUID.randomUUID(), "Berfu", "berfu@gmail.com")
  *
  * // log to console
  * logger.info(user)   //   User(5f3ab309-2784-4b78-a798-6cf44f35edfd, ***, ***)
  * // but accessing the single field will return the real value
  * logger.info(s"the name is ${user.name}") // the name is Berfu
  * }}}
  */
class redacted extends StaticAnnotation
