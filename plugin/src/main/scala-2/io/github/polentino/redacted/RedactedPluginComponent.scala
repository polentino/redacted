package io.github.polentino.redacted

import scala.tools.nsc.backend.jvm.GenBCode
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.util.Success
import scala.tools.nsc.Global

class RedactedPluginComponent(val global: Global) extends PluginComponent with Transform {

  override val phaseName: String = "patch-tostring-component"

  override val runsAfter: List[String] = List("parser")

  override val runsRightAfter: Option[String] = Some("parser")

  import global._

  override protected def newTransformer(unit: CompilationUnit): Transformer = ToStringMaskerTransformer

  private object ToStringMaskerTransformer extends Transformer {

    private val TO_STRING_NAME = "toString"
    private val redactedTypeName = TypeName("redacted")

    override def transform(tree: Tree): Tree = {
      val transformedTree = super.transform(tree)
      validate(transformedTree) match {
        case None => transformedTree
        case Some(validatedClassDef) =>
          val maybePatchedClassDef = for {
            newToStringBody <- createToStringBody(validatedClassDef)
              .withLog(s"couldn't create a valid toString body for ${validatedClassDef.name.decode}")

            newToStringMethod <- buildToStringMethod(newToStringBody)
              .withLog(s"couldn't create a valid toString body for ${validatedClassDef.name.decode}")

            patchedClassDef <- patchCaseClass(validatedClassDef, newToStringMethod)
              .withLog(s"couldn't create a valid toString body for ${validatedClassDef.name.decode}")

          } yield patchedClassDef

          maybePatchedClassDef match {
            case Some(patchedClassDef) => patchedClassDef
            case None =>
              reporter.warning(
                tree.pos,
                s"""
                   |Dang, couldn't patch properly ${tree.symbol.nameString} :(
                   |If you believe this is an error: please report the issue, along with a minimum reproducible example,
                   |at the following link: https://github.com/polentino/redacted/issues/new .
                   |
                   |Thank you 🙏
                   |""".stripMargin
              )
              tree
          }
      }
    }

    /** Utility method that ensures the current tree being inspected is a case class with at least one parameter
      * annotated with `@redacted`.
      * @param tree
      *   the tree to be checked
      * @return
      *   an option containing the validated `ClassDef`, or `None`
      */
    private def validate(tree: Tree): Option[global.ClassDef] = for {
      caseClassType <- validateTypeDef(tree)
      _ <- getRedactedFields(caseClassType)
    } yield caseClassType

    /** Utility method that checks whether the current tree being inspected corresponds to a case class.
      * @param tree
      *   the tree to be checked
      * @return
      *   an option containing the validated `ClassDef`, or `None`
      */
    private def validateTypeDef(tree: Tree): Option[ClassDef] = tree match {
      case classDef: ClassDef if classDef.mods.isCase => Some(classDef)
      case _                                          => None
    }

    /** Utility method that returns all ctor fields annotated with `@redacted`
      * @param classDef
      *   the ClassDef to be checked
      * @return
      *   an Option with the list of all params marked with `@redacted`, or `None` otherwise
      */
    private def getRedactedFields(classDef: ClassDef): Option[List[ValDef]] =
      classDef.impl.body.collectFirst {
        case d: DefDef if d.name.decode == GenBCode.INSTANCE_CONSTRUCTOR_NAME =>
          d.vparamss.headOption.fold(List.empty[ValDef])(v => v.filter(_.mods.hasAnnotationNamed(redactedTypeName)))
      }

    /** Utility method to generate a new `toString` definition based on the parameters marked with `@redacted`.
      * @param classDef
      *   the ClassDef for which we need a dedicated `toString` method
      * @return
      *   the body of the new `toString` method
      */
    private def createToStringBody(classDef: ClassDef): scala.util.Try[Tree] = scala.util.Try {
      val className = classDef.name.decode
      val memberNames = getAllFields(classDef)
      val classPrefix = (className + "(").toConstantLiteral
      val classSuffix = ")".toConstantLiteral
      val commaSymbol = ",".toConstantLiteral
      val asterisksSymbol = "***".toConstantLiteral
      val concatOperator = TermName("$plus")

      val fragments: List[Tree] = memberNames.map(m =>
        if (m.mods.hasAnnotationNamed(redactedTypeName)) asterisksSymbol
        else Apply(Select(Ident(m.name), TO_STRING_NAME), Nil))

      def buildToStringTree(fragments: List[Tree]): Tree = {

        def concatAll(l: List[Tree]): List[Tree] = l match {
          case Nil          => Nil
          case head :: Nil  => List(head)
          case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
        }

        val res = concatAll(fragments).fold(classPrefix) { case (accumulator, fragment) =>
          Apply(Select(accumulator, concatOperator), List(fragment))
        }
        Apply(Select(res, concatOperator), List(classSuffix))
      }

      buildToStringTree(fragments)
    }

    /** Returns all the fields in a case class ctor.
      * @param classDef
      *   the `ClassDef` for which we want to get all if ctor field
      * @return
      *   a list of all the `ValDef`
      */
    private def getAllFields(classDef: ClassDef): List[ValDef] =
      classDef.impl.body.collectFirst {
        case d: DefDef if d.name.decode == GenBCode.INSTANCE_CONSTRUCTOR_NAME => d.vparamss.headOption.getOrElse(Nil)
      }.getOrElse(Nil)

    /** Build a new `toString` method definition containing the body passed as parameter.
      * @param body
      *   the body of the newly created `toString` method
      * @return
      *   the whole `toString` method definition
      */
    private def buildToStringMethod(body: Tree): scala.util.Try[DefDef] = scala.util.Try {
      DefDef(Modifiers(Flag.OVERRIDE), TermName(TO_STRING_NAME), Nil, Nil, TypeTree(), body)
    }

    /** Utility method that adds a new method definition to an existing `ClassDef` body.
      * @param classDef
      *   the class that needs to be patched
      * @param newToStringMethod
      *   the new method that will be included in the `ClassDef` passed as first parameter
      * @return
      *   the patched `ClassDef`
      */
    private def patchCaseClass(classDef: ClassDef, newToStringMethod: Tree): scala.util.Try[ClassDef] =
      scala.util.Try {
        val newBody = classDef.impl.body :+ newToStringMethod
        val newImpl = classDef.impl.copy(body = newBody)
        classDef.copy(impl = newImpl)
      }

    // utility extension classes

    private implicit class AstOps(s: String) {
      def toConstantLiteral: Literal = Literal(Constant(s))
    }

    private implicit class TryOps[Out](opt: scala.util.Try[Out]) {

      def withLog(message: String): Option[Out] = opt match {
        case Success(value) => Some(value)
        case _              => reporter.echo(message); None
      }
    }
  }
}
