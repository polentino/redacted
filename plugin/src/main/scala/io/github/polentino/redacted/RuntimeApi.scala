package io.github.polentino.redacted

import scala.util.{Failure, Success}

trait RuntimeApi {
  type Tree
  type MethodDef <: Tree
  type Symbol
  type Literal <: Tree
  type Position
  type TermName
  protected val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"

  final case class ValidationResult(owner: Symbol, toStringDef: MethodDef, redactedFields: List[Symbol])

  final def process(tree: Tree): Option[Tree] = for {
    validationResult <- validate(tree)
    _ = println(s"BUILDING TO STRING BODY FOR ${getOwnerName(tree)}")
    body <- createToStringBody(validationResult) match {
      case Failure(exception) =>
        println(s"createToStringBody failed for ${getOwnerName(tree)}: ${exception.printStackTrace()}")
        None
      case Success(value) => Some(value)
    }
    _ = println(s"BUILT TO STRING BODY FOR ${getOwnerName(tree)}")
    newMethodDefinition <- patchToString(validationResult.toStringDef, body) match {
      case Failure(exception) =>
        println(s"patchToString failed for ${getOwnerName(tree)}: ${exception.printStackTrace()}")
        None
      case Success(value) => Some(value)
    }
    _ = println(s"PATCHED ${getOwnerName(tree)}")
    _ = println(newMethodDefinition.toString)
  } yield newMethodDefinition

  protected def validate(tree: Tree): Option[ValidationResult]

  private def createToStringBody(validationResult: ValidationResult): util.Try[Tree] = util.Try {
    val className = getOwnerName(validationResult.toStringDef)
    val memberNames = getOwnerMembers(validationResult.owner)
    val classPrefix = toConstantLiteral(className + "(")
    val classSuffix = toConstantLiteral(")")
    val commaSymbol = toConstantLiteral(",")
    val asterisksSymbol = toConstantLiteral("***")

    val fragments = memberNames.map(field =>
      if (validationResult.redactedFields.contains(field)) asterisksSymbol
      else selectField(validationResult.owner, field))

    def buildToStringTree(fragments: List[Tree]): Tree = {

      def concatAll(l: List[Tree]): List[Tree] = l match {
        case Nil          => Nil
        case head :: Nil  => List(head)
        case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
      }

      val res = concatAll(fragments).fold(classPrefix) { case (accumulator, fragment) =>
        concat(accumulator, concatOperator, fragment)
      }
      concat(res, concatOperator, classSuffix)
    }

    buildToStringTree(fragments)
  }

  protected def extractMethodDefinition(tree: Tree): Option[MethodDef]
  protected def isToString(defDef: MethodDef): Option[MethodDef]
  protected def getRedactedFields(tree: Tree): Option[List[Symbol]]
  protected def getOwnerName(tree: Tree): String
  protected def getOwnerMembers(owner: Symbol): List[Symbol]
  protected def toConstantLiteral(name: String): Literal
  protected def concatOperator: TermName
  protected def selectField(owner: Symbol, field: Symbol): Tree
  protected def concat(lhs: Tree, concatOperator: TermName, rhs: Tree): Tree
  protected def patchToString(defDef: MethodDef, newToStringBody: Tree): scala.util.Try[MethodDef]
}
