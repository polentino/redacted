package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.{Flags, Symbols}

object AstOps {
  private val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"

  def redactedSymbol(using Context): ClassSymbol = Symbols.requiredClass(REDACTED_CLASS)

  extension (s: String)(using Context) {
    def toConstantLiteral: tpd.Tree = tpd.Literal(Constant(s))
  }

  extension (symbol: Symbol)(using Context) {

    def redactedFields: List[String] = {
      val redactedType = redactedSymbol
      symbol.primaryConstructor.paramSymss.headOption.fold(List.empty[String]) { params =>
        params
          .filter(_.annotations.exists(_.matches(redactedType)))
          .map(_.name.toString)
      }
    }
  }

  extension (tree: tpd.TypeDef)(using Context) {
    def isCaseClass: Boolean = tree.symbol.is(Flags.CaseClass)
  }
}
