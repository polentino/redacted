package io.github.polentino.redacted

import scala.tools.nsc.backend.jvm.GenBCode
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.util.{Failure, Success}
import scala.tools.nsc.Global

class RedactedPluginComponent(val global: Global) extends PluginComponent with Transform {

  override val phaseName: String = "patch-tostring-component"

  override val runsAfter: List[String] = List("typer") // todo typer or not?

//  override val runsRightAfter: Option[String] = Some("parser")

  import global._

  override protected def newTransformer(unit: CompilationUnit): Transformer = ToStringMaskerTransformer

  private object ToStringMaskerTransformer extends Transformer {
    private val REDACTED_CLASS: String = "io.github.polentino.redacted.redacted"
    private val TO_STRING_NAME = "toString"
    private val redactedTypeName = TypeName("redacted")
    private val stringType = global.definitions.StringTpe

    override def transform(tree: Tree): Tree = {
      val t = super.transform(tree)
      validate2(t) match {
        case Some(validatedDefDef) =>
          val mods = validatedDefDef.mods
          val name = validatedDefDef.name
          val tparams = validatedDefDef.tparams
          val vparamss = validatedDefDef.vparamss
          val rhs = validatedDefDef.rhs
          val tpt = validatedDefDef.tpt

          val res = for {
            body <- createToStringBody(validatedDefDef)
          } yield treeCopy.DefDef(validatedDefDef, mods, name, tparams, vparamss, tpt, body)


          res.get
        case None => t
      }
    }

    private def validate2(tree: Tree): Option[DefDef] = for {
      defDef <- isToString(tree)
      _ <- ownerRedactedFields(tree.symbol.owner)
    } yield defDef

    private def isToString(tree: Tree) = tree match {
      case d:DefDef if d.name == TermName("toString") && d.symbol.owner.isCase => Some(d)
      case _ => None
    }

    private def ownerRedactedFields(symbol: Symbol): Option[List[Symbol]] = {
      symbol
        .primaryConstructor.paramss.flatten
        .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
        case Nil => None
        case redactedFields => Some(redactedFields)
      }
    }

    /** Utility method that ensures the current tree being inspected is a case class with at least one parameter
     * annotated with `@redacted`.
     * @param tree
     *   the tree to be checked
     * @return
     *   an option containing the validated `ClassDef`, or `None`
     */
    private def validate2(template: Template): Option[Template] = for {
      validatedTemplate <- validateTemplate(template)
      _ <- getRedactedFields2(template)
    } yield validatedTemplate

    private def validateTemplate(template: Template): Option[Template] = Option(template.tpe) match {
      case Some(tpe) if tpe.typeSymbol.isCaseClass => Some(template)
      case _ => None
    }

    private def getRedactedFields2(template: Template): Option[List[Symbol]] = {
      template.tpe.typeSymbol
        .primaryConstructor.paramss.flatten
        .filter(_.annotations.exists(_.symbol.fullName == REDACTED_CLASS)) match {
        case Nil => None
        case redactedFields => Some(redactedFields)
      }
    }

    private def patchToString(template: Template, newToStringBody: Tree): scala.util.Try[Template] = scala.util.Try {
      val newBody = template.body.map {
        case d: DefDef if d.name.decode == TO_STRING_NAME =>
          treeCopy.DefDef(
            d,
            d.mods,
            d.name,
            Nil,
            Nil,
            d.tpt,
            newToStringBody)
        case value => value
      }

      treeCopy.Template(
        template,
        template.parents,
        template.self,
        newBody)
    }

    private def createToStringBody(defDef: DefDef): scala.util.Try[Tree] = scala.util.Try {
      val className = defDef.symbol.owner.unexpandedName.toString // todo replace originalName
      val redactedFields = ownerRedactedFields(defDef.symbol.owner).getOrElse(Nil)
      val memberNames = defDef.symbol.owner.primaryConstructor.paramss.headOption.getOrElse(Nil)
      val classPrefix = (className + "(").toConstantLiteral.setType(stringType)
      val classSuffix = ")".toConstantLiteral.setType(stringType)
      val commaSymbol = ",".toConstantLiteral.setType(stringType)
      val asterisksSymbol = "***".toConstantLiteral.setType(stringType)
      val concatOperator = TermName("$plus")
      val thisRef: Tree = global.typer.typed(This(defDef.symbol.owner))


      reporter.echo(s"REDACTED FIELDS: ${redactedFields.map(_.nameString)}")
      reporter.echo(s"ALL FIELDS: ${memberNames.map(_.nameString)}")

      val fragments: List[Tree] = memberNames.map(m =>
        if (redactedFields.contains(m)) asterisksSymbol
        else global.typer.typed(Select(thisRef, m.name))) //Apply(Select(thisRef, m.name), Nil))

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

      buildToStringTree(fragments).setType(stringType)
    }

    private def getAllFields2(template: Template): List[Symbol] =
      template.tpe.typeSymbol.primaryConstructor.paramss.flatten

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
      } match {
        case Some(fields) if fields.nonEmpty => Some(fields)
        case _ => None
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
