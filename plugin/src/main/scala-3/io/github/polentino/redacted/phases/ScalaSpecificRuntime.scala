package io.github.polentino.redacted.phases

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

import io.github.polentino.redacted.RuntimeApi

trait ScalaSpecificRuntime extends RuntimeApi {
  protected implicit lazy val context: Context
  type Tree = tpd.Tree
  type MethodDef = tpd.DefDef
  type Symbol = Symbols.Symbol
  type Position = SrcPos
  type Literal = tpd.Literal
  type TermName = Names.TermName
  private val toStringTermName = termName("toString")

  protected def validate(tree: Tree): Option[ValidationResult] = for {
    defDefInCaseClass <- extractMethodDefinition(tree)
    toStringDefDef <- isToString(defDefInCaseClass)
    redactedFields <- getRedactedFields(tree)
  } yield ValidationResult(tree.symbol.owner, toStringDefDef, redactedFields)

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef] = {
    tree match {
      case d: tpd.DefDef if d.symbol.is(Flags.Method) && d.name == toStringTermName => Some(d)
      case _                                                                        => None
    }
  }

  protected def isToString(defDef: MethodDef): Option[MethodDef] =
    Option.when(defDef.name == toStringTermName)(defDef)

  protected def getRedactedFields(tree: Tree): Option[List[Symbol]] = {
    val redactedFields = tree.symbol.owner.primaryConstructor.paramSymss.headOption.fold(List.empty[Symbol]) { params =>
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

  protected def concatOperator: TermName = Names.termName("+")

  protected def selectField(owner: Symbol, field: Symbol): Tree =
    tpd.This(owner.asClass).select(field.name)

  protected def concat(lhs: Tree, concatOperator: TermName, rhs: Tree): Tree = {
    lhs.select(concatOperator).appliedTo(rhs)
  }

  protected def patchToString(defDef: MethodDef, newToStringBody: Tree): scala.util.Try[MethodDef] = scala.util.Try {
    tpd.cpy.DefDef(defDef)(rhs = newToStringBody)
  }
}

object ScalaSpecificRuntime {

  def create(using ctx: Context): ScalaSpecificRuntime = new ScalaSpecificRuntime {
    implicit lazy val context: Context = ctx
  }
}
