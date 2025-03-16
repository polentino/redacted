package io.github.polentino.redacted.runtime

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.*
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.untpd.Modifiers
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.util.Spans.Span
import dotty.tools.dotc.util.SrcPos

import io.github.polentino.redacted.api.internal.RuntimeApi

trait Scala3Runtime extends RuntimeApi {
  protected implicit lazy val context: Context
  type Tree = tpd.Tree
  type DefDef = tpd.DefDef
  type Symbol = Symbols.Symbol
  type Position = SrcPos
  type Literal = tpd.Literal
  type TermName = Names.TermName
  private val toStringTermName = termName("toString")

  protected def getCaseClassOwner(tree: Tree): Option[Symbol] = {
    val owner = tree.symbol.owner
    Option.when(owner.is(Flags.CaseClass))(owner)
  }

  protected def isToString(tree: Tree): Option[DefDef] = tree match {
    case d: tpd.DefDef if d.name == toStringTermName => Some(d)
    case _                                           => None
  }

  protected def getRedactedFields(owner: Symbol): Option[List[Symbol]] = {
    val redactedFields = owner.primaryConstructor.paramSymss.headOption.fold(List.empty[Symbol]) { params =>
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
    }
    Option.when(redactedFields.nonEmpty)(redactedFields)
  }

  protected def getOwnerName(tree: Tree): String =
    tree.symbol.owner.name.toString

  protected def getOwnerMembers(owner: Symbol): List[Symbol] =
    owner.primaryConstructor.paramSymss.headOption.getOrElse(Nil)

  protected def toConstantLiteral(name: String): Literal =
    tpd.Literal(Constants.Constant(name))

  protected def stringConcatOperator: TermName = Names.termName("+")

  protected def selectField(owner: Symbol, field: Symbol): Tree =
    tpd.This(owner.asClass).select(field.name)

  protected def concat(lhs: Tree, stringConcatOperator: TermName, rhs: Tree): Tree = {
    lhs.select(stringConcatOperator).appliedTo(rhs)
  }

  protected def patchToString(toStringDef: DefDef, newToStringBody: Tree): scala.util.Try[DefDef] = scala.util.Try {
    tpd.cpy.DefDef(toStringDef)(rhs = newToStringBody)
  }
}

object Scala3Runtime {

  def create(using ctx: Context): Scala3Runtime = new Scala3Runtime {
    implicit lazy val context: Context = ctx
  }
}
