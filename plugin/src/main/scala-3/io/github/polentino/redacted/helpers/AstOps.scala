package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.{Trees, tpd}
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
      symbol.primaryConstructor.paramSymss.headOption.fold(List.empty[String]) { params =>
        params
          .filter(_.annotations.exists { annotation =>
            annotation.tree match {
              case Trees.Apply(Trees.TypeApply(qualifier, _), _) =>
                qualifier.symbol.maybeOwner.fullName.toString == REDACTED_CLASS
              case Trees.Apply(qualifier, _) =>
                qualifier.symbol.maybeOwner.fullName.toString == REDACTED_CLASS
              case _ => false
            }
          })
          .map(_.name.toString)
      }
    }
  }

  extension (tree: tpd.TypeDef)(using Context) {
    def isCaseClass: Boolean = tree.symbol.is(Flags.CaseClass)
  }
}
