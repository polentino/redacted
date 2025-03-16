package io.github.polentino.redacted

import scala.tools.nsc.Global

// this won't work, because we need a hold of `global` type ahead of creation
// final case class ScalaSpecificRuntime[GlobalRef <: Global](global: Global) extends RuntimeApi { .. }
trait ScalaSpecificRuntime[GlobalRef <: Global] extends RuntimeApi {
  protected val theGlobal: GlobalRef
  type Tree = theGlobal.Tree
  type MethodDef = theGlobal.DefDef
  type Symbol = theGlobal.Symbol
  type Position = theGlobal.Position
  type Literal = theGlobal.Literal
  type TermName = theGlobal.TermName

  // todo: move to RuntimeApi
  private lazy val toStringTermName = theGlobal.TermName("toString")

  protected def validate(tree: Tree): Option[ValidationResult] = for {
    defDefInCaseClass <- extractMethodDefinition(tree)
    toStringDefDef <- isToString(defDefInCaseClass)
    redactedFields <- getRedactedFields(tree)
  } yield ValidationResult(tree.symbol.owner, toStringDefDef, redactedFields)

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef] = tree match {
    case d: MethodDef if d.symbol.owner.isCaseClass => Some(d)
    case _                                          => None
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

  protected def getOwnerName(tree: Tree): String =
    tree.symbol.owner.unexpandedName.toString

  protected def getOwnerMembers(owner: Symbol): List[Symbol] =
    owner.primaryConstructor.paramss.headOption.getOrElse(Nil)

  protected def toConstantLiteral(name: String): Literal = {
    import theGlobal._
    theGlobal.Literal(Constant(name))
  }

  protected def concatOperator: TermName = {
    import theGlobal._
    theGlobal.TermName("$plus")
  }

  protected def selectField(owner: Symbol, field: Symbol): Tree = {
    import theGlobal._
    val thisRef = theGlobal.typer.typed(This(owner))
    theGlobal.typer.typed(Select(thisRef, field.name))
  }

  protected def concat(lhs: Tree, concatOperator: TermName, rhs: Tree): Tree = {
    import theGlobal._
    theGlobal.typer.typed(Apply(Select(lhs, concatOperator), List(rhs)))
  }

  protected def patchToString(defDef: MethodDef, newToStringBody: Tree): scala.util.Try[MethodDef] = scala.util.Try {
    theGlobal.treeCopy.DefDef(
      defDef,
      defDef.mods,
      defDef.name,
      defDef.tparams,
      defDef.vparamss,
      defDef.tpt,
      newToStringBody)
  }
}

object ScalaSpecificRuntime {

  def apply(global: Global): ScalaSpecificRuntime[global.type] = new ScalaSpecificRuntime[global.type] {
    override protected val theGlobal: global.type = global
    import theGlobal._
  }
}
