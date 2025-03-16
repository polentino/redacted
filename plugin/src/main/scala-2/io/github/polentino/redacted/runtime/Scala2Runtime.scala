package io.github.polentino.redacted.runtime

import scala.tools.nsc.Global

import io.github.polentino.redacted.api.internal._

// this won't work, because we need a hold of `global` type ahead of creation
// final case class ScalaSpecificRuntime[GlobalRef <: Global](global: Global) extends RuntimeApi { .. }
trait Scala2Runtime[GlobalRef <: Global] extends RuntimeApi {
  protected val theGlobal: GlobalRef
  override type Tree = theGlobal.Tree
  override type DefDef = theGlobal.DefDef
  override type Symbol = theGlobal.Symbol
  override type Position = theGlobal.Position
  override type Literal = theGlobal.Literal
  override type TermName = theGlobal.TermName

  private lazy val toStringTermName = theGlobal.TermName(TO_STRING_NAME)

  override protected def caseClassOwner(tree: Tree): Option[Symbol] =
    Option(tree.symbol).collectFirst { case symbol if symbol.owner.isCaseClass => symbol.owner }

  override protected def isToString(tree: Tree): Option[DefDef] = tree match {
    case d: DefDef if d.name == toStringTermName => Some(d)
    case _                                       => None
  }

  override protected def redactedFields(owner: Symbol): Option[List[Symbol]] =
    owner
      .primaryConstructor
      .paramss
      .flatten
      .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
      case Nil            => None
      case redactedFields => Some(redactedFields)
    }

  override protected def ownerName(tree: Tree): String =
    tree.symbol.owner.unexpandedName.toString

  override protected def constructorFields(owner: Symbol): List[Symbol] =
    owner.primaryConstructor.paramss.headOption.getOrElse(Nil)

  override protected def constantLiteral(name: String): Literal = {
    import theGlobal._
    theGlobal.Literal(Constant(name))
  }

  override protected def stringConcatOperator: TermName =
    theGlobal.TermName("$plus")

  override protected def selectField(owner: Symbol, field: Symbol): Tree = {
    import theGlobal._
    val thisRef = theGlobal.typer.typed(This(owner))
    theGlobal.typer.typed(Select(thisRef, field.name))
  }

  override protected def concat(lhs: Tree, stringConcatOperator: TermName, rhs: Tree): Tree = {
    import theGlobal._
    theGlobal.typer.typed(Apply(Select(lhs, stringConcatOperator), List(rhs)))
  }

  override protected def patchToString(
    toStringDef: DefDef,
    newToStringBody: Tree
  ): scala.util.Try[DefDef] = scala.util.Try {
    theGlobal.treeCopy.DefDef(
      toStringDef,
      toStringDef.mods,
      toStringDef.name,
      toStringDef.tparams,
      toStringDef.vparamss,
      toStringDef.tpt,
      newToStringBody)
  }

  override protected def treeName(tree: Tree): String =
    tree.symbol.nameString

  override protected def treePos(tree: Tree): Position =
    tree.pos
}

object Scala2Runtime {

  def create(global: Global): Scala2Runtime[global.type] = new Scala2Runtime[global.type] { self =>
    override protected val theGlobal: global.type = global

    override protected val reporterApi: ReporterApi[self.type] = new ReporterApi[self.type] {
      protected val runtime: self.type = self

      override def echo(message: String): Unit = theGlobal.reporter.echo(message)
      override def warning(message: String, pos: runtime.Position): Unit = theGlobal.reporter.warning(pos, message)
    }
  }
}
