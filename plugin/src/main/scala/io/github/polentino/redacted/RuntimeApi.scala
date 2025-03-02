package io.github.polentino.redacted

trait RuntimeApi {
  type Tree
  type MethodDef
  type Symbol
  type Position

  final def process(tree: Tree): Tree = validate(tree) match {
    case Some(_) =>
      println("TREE IS VALID")
      tree
    case None =>
      println("TREE IS NOT VALID")
      tree
  }

  private final def validate(tree: Tree): Option[MethodDef] = for {
    defDefInCaseClass <- extractMethodDefinition(tree)
    toStringDefDef <- isToString(defDefInCaseClass)
    _ <- getRedactedFields(tree)
  } yield toStringDefDef

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef]
  protected def isToString(defDef: MethodDef): Option[MethodDef]
  protected def getRedactedFields(symbol: Tree): Option[List[Symbol]]
}
