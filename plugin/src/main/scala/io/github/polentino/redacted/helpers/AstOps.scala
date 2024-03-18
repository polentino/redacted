package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.{Flags, Symbols}

import io.github.polentino.redacted.redacted

object AstOps {
  private val REDACTED_CLASS: String = classOf[redacted].getCanonicalName

  def redactedSymbol(using Context): ClassSymbol = Symbols.requiredClass(REDACTED_CLASS)

  extension (s: String)(using Context) {
    def toConstantLiteral: tpd.Tree = tpd.Literal(Constant(s))
  }

  extension (symbol: Symbol)(using Context) {
    def isCompanionObject: Boolean = symbol.isAllOf(Flags.SyntheticModule) || symbol.is(Flags.Module)
    def hasCompanionCaseClass: Boolean = symbol.linkedClass.is(Flags.CaseClass)

    def redactedFields: List[String] = {
      val redactedType = redactedSymbol
      symbol.primaryConstructor.paramSymss.flatten.collect {
        case s if s.annotations.exists(_.matches(redactedType)) => s.name.toString
      }
    }
  }

  extension (tree: tpd.TypeDef)(using Context) {
    def isCaseClass: Boolean = tree.symbol.is(Flags.CaseClass)
    def isCompanionObject: Boolean = tree.symbol.isCompanionObject
    def hasCompanionCaseClass: Boolean = tree.symbol.hasCompanionCaseClass
  }
}
