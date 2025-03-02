package io.github.polentino.redacted

import scala.tools.nsc.Global

// this won't work, because we need a hold of `global` type ahead of creation
// final case class ScalaSpecificRuntime[GlobalRef <: Global](global: Global) extends RuntimeApi { .. }
trait ScalaSpecificRuntime[GlobalRef <: Global] extends RuntimeApi {
  protected val theGlobal: GlobalRef
  override type Tree = theGlobal.Tree
  override type MethodDef = theGlobal.DefDef
  override type Symbol = theGlobal.Symbol
  override type Position = theGlobal.Position

  // todo: move to RuntimeApi
  private val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"
  private lazy val toStringTermName = theGlobal.TermName("toString")

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef] = tree match {
    case d: MethodDef if d.symbol.owner.isCaseClass => Some(d)
    case _                                       => None
  }

  protected def isToString(defDef: MethodDef): Option[MethodDef] =
    if (defDef.name == toStringTermName) Some(defDef) else None

  protected def getRedactedFields(tree: Tree): Option[List[Symbol]] =
    tree.symbol.owner
      .primaryConstructor
      .paramss
      .flatten
      .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
      case Nil            => None
      case redactedFields => Some(redactedFields)
    }
}

object ScalaSpecificRuntime {

  def apply(global: Global): ScalaSpecificRuntime[global.type] = new ScalaSpecificRuntime[global.type ] {
    override protected val theGlobal: global.type  = global
    import global._
  }
}
