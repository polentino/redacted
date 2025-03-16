package io.github.polentino.redacted.runtime

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.*
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.util.SrcPos

import io.github.polentino.redacted.api.internal.*

trait Scala3Runtime extends RuntimeApi {
  protected implicit lazy val context: Context
  override type Tree = tpd.Tree
  override type DefDef = tpd.DefDef
  override type Symbol = Symbols.Symbol
  override type Position = SrcPos
  override type Literal = tpd.Literal
  override type TermName = Names.TermName

  private val toStringTermName = termName(TO_STRING_NAME)

  override protected def caseClassOwner(tree: Tree): Option[Symbol] =
    Option(tree.symbol).collectFirst { case symbol if symbol.owner.is(Flags.CaseClass) => symbol.owner }

  override protected def extractToString(tree: Tree): Option[DefDef] = tree match {
    case d: tpd.DefDef if d.name == toStringTermName => Some(d)
    case _                                           => None
  }

  override protected def redactedFields(owner: Symbol): Option[List[Symbol]] = {
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

  override protected def ownerName(tree: Tree): String =
    tree.symbol.owner.name.toString

  override protected def constructorFields(owner: Symbol): List[Symbol] =
    owner.primaryConstructor.paramSymss.headOption.getOrElse(Nil)

  override protected def constantLiteral(name: String): Literal =
    tpd.Literal(Constants.Constant(name))

  override protected def stringConcatOperator: TermName =
    Names.termName("+")

  override protected def selectField(owner: Symbol, field: Symbol): Tree =
    tpd.This(owner.asClass).select(field.name)

  override protected def concat(lhs: Tree, stringConcatOperator: TermName, rhs: Tree): Tree =
    lhs.select(stringConcatOperator).appliedTo(rhs)

  override protected def patchToString(toStringDef: DefDef, newToStringBody: Tree): scala.util.Try[DefDef] =
    scala.util.Try { tpd.cpy.DefDef(toStringDef)(rhs = newToStringBody) }

  override protected def treeName(tree: Tree): String =
    tree.symbol.name.toString

  override protected def treePos(tree: Tree): Position =
    tree.srcPos
}

object Scala3Runtime {

  def create(using ctx: Context): Scala3Runtime = new Scala3Runtime { self =>
    implicit lazy val context: Context = ctx

    override protected val reporterApi: ReporterApi[self.type] = new ReporterApi[self.type] {
      protected val runtime = self

      override def echo(message: String): Unit = report.echo(message)
      override def warning(message: String, pos: runtime.Position): Unit = report.warning(message, pos)
    }
  }
}
