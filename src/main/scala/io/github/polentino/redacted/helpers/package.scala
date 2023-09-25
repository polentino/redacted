package io.github.polentino.redacted

package object helpers {

  private val ASTERISKS = "***"

  def toRedactedString(x: Product, redactedFields: String*): String =
    (0 until x.productArity).map { index =>
      if (redactedFields.contains(x.productElementName(index))) ASTERISKS
      else x.productElement(index)
    }.mkString(x.productPrefix + "(", ",", ")")
}
