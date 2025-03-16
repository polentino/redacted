package io.github.polentino.redacted.runtime

import scala.tools.nsc.Global

import io.github.polentino.redacted.api.internal.RuntimeApi

// this won't work, because we need a hold of `global` type ahead of creation
// final case class ScalaSpecificRuntime[GlobalRef <: Global](global: Global) extends RuntimeApi { .. }
trait Scala2Runtime[GlobalRef <: Global] extends RuntimeApi {
  protected val theGlobal: GlobalRef
  type Tree = theGlobal.Tree
  type DefDef = theGlobal.DefDef
  type Symbol = theGlobal.Symbol
  type Position = theGlobal.Position
  type Literal = theGlobal.Literal
  type TermName = theGlobal.TermName

  // todo: move to RuntimeApi
  private lazy val toStringTermName = theGlobal.TermName("toString")

  protected def getCaseClassOwner(tree: Tree): Option[Symbol] =
    Option(tree.symbol).collectFirst { case symbol if symbol.owner.isCaseClass => symbol.owner }

  protected def isToString(tree: Tree): Option[DefDef] = tree match {
    case d: DefDef if d.name == toStringTermName => Some(d)
    case _                                       => None
  }

  protected def getRedactedFields(owner: Symbol): Option[List[Symbol]] =
    owner
      .primaryConstructor
      .paramss
      .flatten
      .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
      case Nil            => None
      case redactedFields => Some(redactedFields)
    }

  protected def getOwnerName(tree: Tree): String =
    tree.symbol.owner.unexpandedName.toString

  protected def getOwnerMembers(owner: Symbol): List[Symbol] =
    owner.primaryConstructor.paramss.headOption.getOrElse(Nil)

  protected def toConstantLiteral(name: String): Literal = {
    import theGlobal._
    theGlobal.Literal(Constant(name))
  }

  protected def stringConcatOperator: TermName = {
    theGlobal.TermName("$plus")
  }

  protected def selectField(owner: Symbol, field: Symbol): Tree = {
    import theGlobal._
    val thisRef = theGlobal.typer.typed(This(owner))
    theGlobal.typer.typed(Select(thisRef, field.name))
  }

  protected def concat(lhs: Tree, stringConcatOperator: TermName, rhs: Tree): Tree = {
    import theGlobal._
    theGlobal.typer.typed(Apply(Select(lhs, stringConcatOperator), List(rhs)))
  }

  protected def patchToString(toStringDef: DefDef, newToStringBody: Tree): scala.util.Try[DefDef] = scala.util.Try {
    theGlobal.treeCopy.DefDef(
      toStringDef,
      toStringDef.mods,
      toStringDef.name,
      toStringDef.tparams,
      toStringDef.vparamss,
      toStringDef.tpt,
      newToStringBody)
  }
}

object Scala2Runtime {

  def apply(global: Global): Scala2Runtime[global.type] = new Scala2Runtime[global.type] {
    override protected val theGlobal: global.type = global
  }
}
