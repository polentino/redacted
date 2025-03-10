package io.github.polentino.redacted

trait RuntimeApi {
  type Tree
  type MethodDef
  type Symbol
  type Position
  protected val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"

  final def process(tree: Tree): Tree = validate(tree) match {
    case Some(_) =>
      println(s"${getOwnerName(tree)} can be redacted") // todo use proper logging
      tree
    case None => tree
  }

  private final def validate(tree: Tree): Option[MethodDef] = for {
    defDefInCaseClass <- extractMethodDefinition(tree)
    toStringDefDef <- isToString(defDefInCaseClass)
    _ <- getRedactedFields(tree)
  } yield toStringDefDef

//  private def createToStringBody(defDef: MethodDef): scala.util.Try[Tree] = // todo implement this next :S

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef]
  protected def isToString(defDef: MethodDef): Option[MethodDef]
  protected def getRedactedFields(tree: Tree): Option[List[Symbol]]
  protected def getOwnerName(tree: Tree): String
}
