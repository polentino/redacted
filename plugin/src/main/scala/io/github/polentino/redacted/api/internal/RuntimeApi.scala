package io.github.polentino.redacted.api.internal

import scala.util.{Failure, Success}

/** [[RuntimeApi]] is the main trait to implement in order to provide redaction functionalities for a specific Scala
  * version, such as the type aliases and method utilities to parse Scala's AST for the specific major version of Scala.
  */
trait RuntimeApi {

  /** the FQDN of `@redacted` annotation */
  protected val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"

  /** The AST type of the Tree that will be validated. */
  type Tree

  /** The AST type of the method definition that will be patched. */
  type DefDef <: Tree

  /** The AST type for a Symbol. */
  type Symbol

  /** The AST type for a Literal, such as a character like "(" . */
  type Literal <: Tree

  /** The AST type for a TermName, such as methods like "toString" or ".$plus(..)" . */
  type TermName

  /** The AST type for a position within a source file, useful for logging. */
  type Position

  /** Utility class to wrap all objects needed, when validation succeeds.
    * @param caseClassOwner
    *   the [[Symbol]] referencing the class that owns the `toString` method being analysed
    * @param toStringDef
    *   the [[DefDef]] that represents the definition of the `toString` method being analysed
    * @param redactedFields
    *   a list of [[Symbol]](s) containing all the owner's fields marked with `@redacted`
    */
  private final case class ValidationResult(caseClassOwner: Symbol, toStringDef: DefDef, redactedFields: List[Symbol])

  /** Entry point of the redaction process: the `tree` passed as parameter will be analysed and, if valid, it will be
    * patched and returned to the caller; otherwise, it will be returned unchanged.
    * @param tree
    *   the tree that might get patched
    * @return
    *   the patched (or not) tree
    */
  final def process(tree: Tree): Tree = {
    val newToStringBody = for {
      validationResult <- validate(tree)
      _ = println(s"BUILDING TO STRING BODY FOR ${getOwnerName(tree)}")
      body <- createToStringBody(validationResult) match {
        case Failure(exception) =>
          println(s"createToStringBody failed for ${getOwnerName(tree)}: ${exception.printStackTrace()}")
          None
        case Success(value) => Some(value)
      }
      _ = println(s"BUILT TO STRING BODY FOR ${getOwnerName(tree)}")
      newDefDefinition <- patchToString(validationResult.toStringDef, body) match {
        case Failure(exception) =>
          println(s"patchToString failed for ${getOwnerName(tree)}: ${exception.printStackTrace()}")
          None
        case Success(value) => Some(value)
      }
      _ = println(s"PATCHED ${getOwnerName(tree)}")
      _ = println(newDefDefinition.toString)
    } yield newDefDefinition

    newToStringBody match {
      case Some(value) => value
      case None        =>
//        report.warning(
//          s"""
//             |Dang, couldn't patch properly ${tree.name} :(
//             |If you believe this is an error: please report the issue, along with a minimum reproducible example,
//             |at the following link: https://github.com/polentino/redacted/issues/new .
//             |
//             |Thank you 🙏
//             |""".stripMargin,
//          tree.srcPos
//        )

        tree
    }
  }

  /** Given the `tree` passed as parameter, returns a [[ValidationResult]] if the tree:
    *
    *   - it is contained inside a `case class`
    *   - it is the representation of a `toString` method
    *   - the case class that owns it has at least one field annotated with `@redacted`
    *
    * @param tree
    *   the [[Tree]] to be validated
    * @return
    *   a [[ValidationResult]] object containing the objects needed to begin the redaction process
    */
  private def validate(tree: Tree): Option[ValidationResult] = for {
    owner <- getCaseClassOwner(tree)
    toStringDefDef <- isToString(tree)
    redactedFields <- getRedactedFields(owner)
  } yield ValidationResult(owner, toStringDefDef, redactedFields)

  /** Bulk of the redaction process: here we build up the new body implementation of the `toString` method, based on the
    * fields contained in the [[ValidationResult]] in the following form
    * {{{
    *  // class example
    *  final case class User(id: Int, @redacted name: String, count: Int)
    *
    *  // the patched `toString` AST equivalent that will be returned
    *  def toString = "User(" + User.this.id + "," + "***" + "," + User.this.count + ")"
    * }}}
    * @param result
    *   the validation result
    * @return
    *   the [[Tree]] that represents the patched `toString` body
    */
  private def createToStringBody(result: ValidationResult): util.Try[Tree] = util.Try {
    val ownerClassName = getOwnerName(result.toStringDef)
    val memberNames = getOwnerMembers(result.caseClassOwner)
    val classPrefix = toConstantLiteral(ownerClassName + "(")
    val classSuffix = toConstantLiteral(")")
    val commaSymbol = toConstantLiteral(",")
    val asterisksSymbol = toConstantLiteral("***")

    val fragments = memberNames.map(field =>
      if (result.redactedFields.contains(field)) asterisksSymbol
      else selectField(result.caseClassOwner, field))

    def buildToStringTree(fragments: List[Tree]): Tree = {

      def concatAll(l: List[Tree]): List[Tree] = l match {
        case Nil          => Nil
        case head :: Nil  => List(head)
        case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
      }

      val res = concatAll(fragments).fold(classPrefix) { case (accumulator, fragment) =>
        concat(accumulator, stringConcatOperator, fragment)
      }
      concat(res, stringConcatOperator, classSuffix)
    }

    buildToStringTree(fragments)
  }

  /** Given a [[Tree]] passed as parameter, tries to retrieve its `case class` owner.
    * @param tree
    *   for which we would like to retrieve its case class owner
    * @return
    *   an [[Option]] containing either the [[Symbol]] of its owner, or [[None]]
    */
  protected def getCaseClassOwner(tree: Tree): Option[Symbol]

  /** Given a [[Tree]] passed as parameter, this method tries to convert it (via pattern-match) into a DefDef if it
    * actually represents a `toString` method.
    * @param tree
    *   the tree to be parsed
    * @return
    *   an [[Option]] containing either the [[DefDef]] of the `toString` method, or [[None]]
    */
  protected def isToString(tree: Tree): Option[DefDef]

  /** Returns a list with all the [[Symbol]](s) annotated with `@redacted` from the given `owner`
    *
    * @param owner
    *   the [[Symbol]] representing a `case class` reference for which we want all its `@redacted` fields
    * @return
    *   a non-empty [[List List[Symbol]]] with all the fields that are annotated with `@redacted`
    */
  protected def getRedactedFields(owner: Symbol): Option[List[Symbol]]

  /** Given a `tree`, it returns the name of its owner (regardless whether it is a `case class` or something else)
    * @param tree
    *   the [[Tree]] for which we would like to know its name
    * @return
    *   the [[String]] representation of the owner's name
    */
  protected def getOwnerName(tree: Tree): String

  /** Given a `tree`, it returns a [[List List[Symbol]]] containing all the constructor's fields of the `owner`
    * @param owner
    *   the [[Symbol]] of the related `case class`
    * @return
    *   the [[List List[Symbol]]] of all the fields in the `owner`s' constructor
    */
  protected def getOwnerMembers(owner: Symbol): List[Symbol]

  /** Build a [[Literal]] constant from the given `name` identifier i.e. "("
    * @param name
    *   the String reference of the constant we would like to build
    * @return
    *   the [[Literal]] reference
    */
  protected def toConstantLiteral(name: String): Literal

  /** @return
    *   the [[TermName]] that is used to concatenate two [[String]]s, i.e. "+" or ".$plus"
    */
  protected def stringConcatOperator: TermName

  /** Given a [[Symbol]] that represents the `owner` case class, and a [[Symbol]] that represents the constructor
    * `field` inside it, it combines the two symbols in one [[Tree]] that is equivalent to selecting the field in the
    * case class.
    * {{{
    *  // class example
    *  final case class User(id: Int, name: String, count: Int)
    *
    *  // can build some AST equivalent of
    *    `User.this.id` or
    *    `User.this.name` or
    *    `User.this.count`
    * }}}
    * @param owner
    *   the [[Symbol]] that references the owning `case class`
    * @param field
    *   the [[Symbol]] that reference one specific field in the `owner`'s constructor
    * @return
    *   the [[Tree]] that select the `field` within the `owner`
    */
  protected def selectField(owner: Symbol, field: Symbol): Tree

  /** Given two [[Tree]]s, combine them together using the [[String String.+(..)]] operator, to concatenate them
    * together
    * @param lhs
    *   the left-hand-sde [[Tree]]
    * @param stringConcatOperator
    *   the [[String]] operator for concatenating two items
    * @param rhs
    *   the right-hand-sde [[Tree]]
    * @return
    *   the [[Tree]] that represents the concatenation of the two items
    */
  protected def concat(lhs: Tree, stringConcatOperator: TermName, rhs: Tree): Tree

  /** Given the [[DefDef]] that represents the original `toString` implementation, and the [[Tree]] that represents its
    * new body implementation, patch the new body into the existing definition, and return it.
    * @param toStringDef
    *   the [[DefDef]] which represents the original `toString` AST
    * @param newToStringBody
    *   the [[Tree]] that provides redaction for the sensitive fields
    * @return
    *   the [[DefDef]] that will contain the patched body
    */
  protected def patchToString(toStringDef: DefDef, newToStringBody: Tree): scala.util.Try[DefDef]
}
