package io.github.polentino.redacted

import scala.util.Try

final case class Api () extends RedactedAPI {
  override type Tree = scala.reflect.internal.Trees#Tree
  override type ValidatedTree = scala.reflect.internal.Trees#Tree
  override type Pos = reporter.type#Pos

  override protected lazy val reporter: ReporterApi = new ReporterApi {
    override type Pos = scala.reflect.internal.Positions#Position

    override def echo(message: String): Unit = ???

    override def warning(pos: Pos, message: String): Unit = ???
  }

  override protected def validate(tree: Tree): Option[ValidatedTree] = ???

  override protected def createToStringBody(defDef: ValidatedTree): Try[Tree] = ???

  override protected def patchToString(defDef: Tree, newToStringBody: Tree): Try[Tree] = ???

  override protected def getPos(tree: Tree): Pos = ???

  override protected def getName(tree: Tree): String = ???
}
