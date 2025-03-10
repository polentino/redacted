package io.github.polentino.redacted.phases

import dotty.tools.dotc.ast.{Trees, tpd}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Names.termName
import dotty.tools.dotc.core.{Flags, Symbols}
import dotty.tools.dotc.report
import dotty.tools.dotc.util.SrcPos

import io.github.polentino.redacted.RuntimeApi

trait ScalaSpecificRuntime extends RuntimeApi {
  protected implicit lazy val context: Context

  override type Tree = tpd.DefDef
  override type MethodDef = Tree
  override type Symbol = Symbols.Symbol
  override type Position = SrcPos
  private val toStringTypeName = termName("toString")

  override protected def extractMethodDefinition(tree: Tree): Option[MethodDef] = {
    Option.when(tree.symbol.owner.is(Flags.CaseClass))(tree)
  }

  override protected def isToString(defDef: MethodDef): Option[MethodDef] =
    Option.when(defDef.name == toStringTypeName)(defDef)

  override protected def getRedactedFields(tree: Tree): Option[List[Symbol]] = {
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

}

object ScalaSpecificRuntime {

  def create(using ctx: Context): ScalaSpecificRuntime = new ScalaSpecificRuntime {
    override implicit lazy val context: Context = ctx
  }
}
