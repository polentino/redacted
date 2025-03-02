package io.github.polentino.redacted

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.util.Success
import scala.tools.nsc.Global

class RedactedPluginComponent(val global: Global) extends PluginComponent with Transform {

  override val phaseName: String = "patch-tostring-component"

  override val runsAfter: List[String] = List("parser")

  import global._
  val runtimeApi: ScalaSpecificRuntime[global.type] = ScalaSpecificRuntime(global)
  val redactedApi: RedactedApi[runtimeApi.type] = RedactedApi(runtimeApi)

  override protected def newTransformer(unit: CompilationUnit): Transformer = new RedactedToStringTransformer()

  private class RedactedToStringTransformer() extends Transformer {
    private val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"
    private val toStringTermName = TermName("toString")

    override def transform(tree: Tree): Tree = {
      val transformedTree = super.transform(tree)

      redactedApi.process(transformedTree) // it compiles !

      validate(transformedTree) match {
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
                tree.pos,
                s"""
                   |Dang, couldn't patch properly ${tree.symbol.name} :(
                   |If you believe this is an error: please report the issue, along with a minimum reproducible example,
                   |at the following link: https://github.com/polentino/redacted/issues/new .
                   |
                   |Thank you 🙏
                   |""".stripMargin
              )
              transformedTree
          }

        case None => transformedTree
      }
    }

    /** Ensures that the `Tree` passed as parameter is owned by a case class, its actual type is a `DefDef` of the
      * method `toString`, and that the case class has actually at least one ctor member that is redacted.
      * @param tree
      *   the tree to be validated
      * @return
      *   the validated `DefDef` of a `toString` method definition which is contained in a case class that has at least
      *   one redacted ctor member wrapped in an Option
      */
    private def validate(tree: Tree): Option[DefDef] = for {
      defDefInCaseClass <- extractMethodDefinition(tree)
      toStringDefDef <- isToString(defDefInCaseClass)
      _ <- getRedactedFields(tree.symbol.owner)
    } yield toStringDefDef

    /** Ensures that the current Tree is actually a method definition, and its owner is a case class
      * @param tree
      *   the tree to be validated
      * @return
      *   the `DefDef` derived from the current tree, if it actually is of type `DefDef` and if the owner is a case
      *   class wrapped in an Option
      */
    private def extractMethodDefinition(tree: Tree): Option[DefDef] = tree match {
      case d: DefDef if d.symbol.owner.isCaseClass => Some(d)
      case _                                       => None
    }

    /** Ensures that the current `DefDef` refers to the definition of a `toString` method
      * @param defDef
      *   the `DefDef` to be checked the tree from
      * @return
      */
    private def isToString(defDef: DefDef): Option[DefDef] =
      if (defDef.name == toStringTermName) Some(defDef) else None

    /** Given a `Symbol`, extracts all ctor members annotated with `@redacted`
      * @param symbol
      *   the symbol to be inspected
      * @return
      *   A non-empty list of soon-to-be-redacted symbols, wrapped in an Option
      */
    private def getRedactedFields(symbol: Symbol): Option[List[Symbol]] =
      symbol
        .primaryConstructor
        .paramss
        .flatten
        .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
        case Nil            => None
        case redactedFields => Some(redactedFields)
      }

    /** Builds the new body of the `toString` method
      * @param defDef
      *   the initial `toString` method definition
      * @return
      *   the new body of the `toString` method, that will hide redacted fields
      */
    private def createToStringBody(defDef: DefDef): scala.util.Try[Tree] = scala.util.Try {
      val className = defDef.symbol.owner.unexpandedName.toString
      val redactedFields = getRedactedFields(defDef.symbol.owner).getOrElse(Nil)
      val memberNames = defDef.symbol.owner.primaryConstructor.paramss.headOption.getOrElse(Nil)
      val classPrefix = (className + "(").toConstantLiteral
      val classSuffix = ")".toConstantLiteral
      val commaSymbol = ",".toConstantLiteral
      val asterisksSymbol = "***".toConstantLiteral
      val concatOperator = TermName("$plus")
      val thisRef: Tree = global.typer.typed(This(defDef.symbol.owner))

      val fragments: List[Tree] = memberNames.map(m =>
        if (redactedFields.contains(m)) asterisksSymbol
        else global.typer.typed(Select(thisRef, m.name)))

      def buildToStringTree(fragments: List[Tree]): Tree = {

        def concatAll(l: List[Tree]): List[Tree] = l match {
          case Nil          => Nil
          case head :: Nil  => List(head)
          case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
        }

        val res = concatAll(fragments).fold(classPrefix) { case (accumulator, fragment) =>
          global.typer.typed(Apply(Select(accumulator, concatOperator), List(fragment)))
        }
        global.typer.typed(Apply(Select(res, concatOperator), List(classSuffix)))
      }

      buildToStringTree(fragments)
    }

    /** Utility function to patch the existing `DefDef` with the new body implementation
      *
      * @param defDef
      *   the `DefDef` to be patched
      * @param newToStringBody
      *   the new implementation that needs to override the old one
      * @return
      *   the patched `toString` method definition
      */
    private def patchToString(defDef: DefDef, newToStringBody: Tree): scala.util.Try[DefDef] = scala.util.Try {
      treeCopy.DefDef(
        defDef,
        defDef.mods,
        defDef.name,
        defDef.tparams,
        defDef.vparamss,
        defDef.tpt,
        newToStringBody)
    }

    // utility extension classes

    private implicit class AstOps(s: String) {
      def toConstantLiteral: Literal = Literal(Constant(s))
    }

    private implicit class TryOps[Out](`try`: scala.util.Try[Out]) {

      def withLog(message: String): Option[Out] = `try` match {
        case Success(value) => Some(value)
        case _              => reporter.echo(message); None
      }
    }
  }
}
