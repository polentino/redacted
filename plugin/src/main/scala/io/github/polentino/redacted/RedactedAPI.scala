package io.github.polentino.redacted

import scala.util.{Success, Try}

trait RedactedAPI {
  type Tree
  type ValidatedTree
  type Pos = reporter.Pos

  protected val reporter: ReporterApi

  // Scala 2: Tree => DefDef / Scala 3: tpd.TypeDef => tpd.TypeDef
  def validate(tree: Tree): Option[ValidatedTree]

  // Scala 2: DefDef => Tree / Scala 3: tpd.TypeDef => tpd.Tree
  def createToStringBody(defDef: ValidatedTree): Try[Tree]

  // Scala 2: DefDef => Tree / Scala 3: tpd.TypeDef => tpd.Tree
  def patchToString(defDef: ValidatedTree, newToStringBody: Tree): Try[Tree]

  def getPos(tree: Tree): Pos

  def getName(tree: Tree): String

  final def process(tree: Tree): Tree = validate(tree) match {
    case Some(toStringDef) =>
      val maybePatchedToStringDef = for {
        newToStringBody <- createToStringBody(toStringDef)
          .withLog(s"couldn't create proper `toString()` body")

        patchedToStringDef <- patchToString(toStringDef, newToStringBody)
          .withLog(s"couldn't patch `toString` body into existing method definition")
      } yield patchedToStringDef

      maybePatchedToStringDef match {
        case Some(neeDefDef) => neeDefDef
        case None =>
          reporter.warning(
            getPos(tree),
            s"""
                 |Dang, couldn't patch properly ${getName(tree)} :(
                 |If you believe this is an error: please report the issue, along with a minimum reproducible example,
                 |at the following link: https://github.com/polentino/redacted/issues/new .
                 |
                 |Thank you 🙏
                 |""".stripMargin
          )
          tree
      }

    case None => tree
  }

  private implicit class TryOps[Out](`try`: scala.util.Try[Out]) {

    def withLog(message: String): Option[Out] = `try` match {
      case Success(value) => Some(value)
      case _              => reporter.echo(message); None
    }
  }
}
