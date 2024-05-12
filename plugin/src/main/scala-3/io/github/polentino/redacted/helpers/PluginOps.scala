package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.untpd.Modifiers
import dotty.tools.dotc.ast.{tpd, *}
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.TermName
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.*
import dotty.tools.dotc.util.Spans.Span

import scala.util.{Failure, Success, Try}

import io.github.polentino.redacted.helpers.AstOps.*

object PluginOps {
  private val TO_STRING_NAME = "toString"

  /** Checks whether the `tpd.TypeDef` being inspected:
    *
    *   1. is a case class
    *   1. has fields annotated with `@redacted`
    *
    * and returns an option containing the validated `tpd.TypeDef`.
    *
    * @param tree
    *   the tree to be validated.
    * @param x$2
    *   implicit Context parameter.
    * @return
    *   the tree wrapped in an optional, if valid, or `None` if not.
    */
  def validate(tree: tpd.TypeDef)(using Context): Option[tpd.TypeDef] = for {
    caseClassType <- validateTypeDef(tree)
      .withLogVerbose(s"${tree.name} isn't a case class")
    _ <- getRedactedFields(caseClassType)
      .withLogVerbose(s"${tree.name} doesn't contain `@redacted` fields")
  } yield caseClassType

  /** Checks whether the `tpd.TypeDef` being inspected is case class and, if so, returns it wrapped into an `Option`.
    * @param tree
    *   the tree to be validated.
    * @param x$2
    *   implicit Context parameter.
    * @return
    *   the tree wrapped in an `Option`, if valid, or `None` if not.
    */
  private def validateTypeDef(tree: tpd.TypeDef)(using Context) = Option.when(tree.isCaseClass)(tree)

  /** Checks whether the `tpd.TypeDef` being inspected contains fields annotated with `@redacted` and, if so, returns
    * all of them wrapped into an `Option`.
    * @param tree
    *   the tree to be validated.
    * @param x$2
    *   implicit Context parameter.
    * @return
    *   the list of redacted fields wrapped in an `Option`, if valid, or `None` if no annotate fields were found.
    */
  private def getRedactedFields(tree: tpd.TypeDef)(using Context): Option[List[String]] = {
    val redactedFields = tree.symbol.redactedFields
    Option.when(redactedFields.nonEmpty)(redactedFields)
  }

  /** Does this tree contain a reference to a `tpd.Template`? If so, return it wrapped into an `Option`.
    * @param tree
    *   the tree from which return its `tpd.Template`
    * @param x$2
    *   implicit Context parameter.
    * @return
    *   the template wrapped in an `Option`, if it does exist, or `None` if not.
    */
  def getTreeTemplate(tree: tpd.TypeDef)(using Context): Option[tpd.Template] = tree.rhs match {
    case template: tpd.Template => Some(template)
    case _                      => None
  }

  /** Given the original `tpd.Template` that represents a case class, and a `tpd.Tree` that contains the new body
    * implementation of the `toString` method, this utility function will search for the existing `toString` method
    * defined in the template, and replace the default one (provided by the Scala compiler) with the one passed as
    * second parameter.
    * @param template
    *   the `tpd.Template` for which we want to replace the current `toString` body implementation.
    * @param newToStringBody
    *   the new implementation of the `toString` body.
    * @param x$3
    *   implicit Context parameter.
    * @return
    *   the `tpd.Template` with the updated `toString` method, wrapped in a `Try`.
    */
  def patchToString(template: tpd.Template, newToStringBody: tpd.Tree)(using Context): Try[tpd.Template] = Try {
    val newBody = template.body.map {
      case oldMethodDefinition: tpd.DefDef if oldMethodDefinition.name.toString == TO_STRING_NAME =>
        tpd.cpy.DefDef(oldMethodDefinition)(rhs = newToStringBody)
      case otherField => otherField
    }

    tpd.cpy.Template(template)(body = newBody)
  }

  /** Creates the full body of the new `toString` method. For example, given the following case class
    * {{{
    *   case class User(id: UUID, nickname: String, @redacted email: String)
    * }}}
    *
    * this method will create the AST equivalent of the following string concatenation
    * {{{
    *     "User(" + this.id + "," + this.nickname + "," + "***" + ")"
    * }}}
    * @param tree
    *   the `tpd.Tree` from which we want to derive a customized `toString` body implementation.
    * @param x$2
    *   implicit Context parameter.
    * @return
    *   the `tpd.Tree` of the customized `toString` body implementation, wrapped in a `Try`.
    */
  def createToStringBody(tree: tpd.TypeDef)(using Context): Try[tpd.Tree] = Try {
    val className = tree.name.toString
    val memberNames = tree.symbol.primaryConstructor.paramSymss.headOption.getOrElse(Nil)
    val annotationSymbol = redactedSymbol
    val classPrefix = (className + "(").toConstantLiteral
    val classSuffix = ")".toConstantLiteral
    val commaSymbol = ",".toConstantLiteral
    val asterisksSymbol = "***".toConstantLiteral
    val concatOperator = Names.termName("+")

    val fragments: List[tpd.Tree] = memberNames.map(m =>
      if (m.annotations.exists(_.matches(annotationSymbol))) asterisksSymbol
      else tpd.This(tree.symbol.asClass).select(m.name))

    def buildToStringTree(fragments: List[tpd.Tree]): tpd.Tree = {

      def concatAll(l: List[tpd.Tree]): List[tpd.Tree] = l match {
        case Nil          => Nil
        case head :: Nil  => List(head)
        case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
      }

      val res = concatAll(fragments).fold(classPrefix) { case (l, r) => l.select(concatOperator).appliedTo(r) }
      res.select(concatOperator).appliedTo(classSuffix)
    }

    buildToStringTree(fragments)
  }

  /** Given the original `tpd.TypeDef`, try to replace its `tpd.Template` with the one provided as second argument.
    * @param tree
    *   the original `tpd.TypeDef` that we would like to update..
    * @param newTemplate
    *   the `tpd.Template` that contains the updated `toString` method.
    * @param x$3
    *   implicit Context parameter.
    * @return
    *   the `tpd.TypeDef` with the updated `toString` method, wrapped in a `Try`.
    */
  def patchTypeDef(tree: tpd.TypeDef, newTemplate: tpd.Template)(using Context): Try[tpd.TypeDef] = Try {
    tpd.cpy.TypeDef(tree)(rhs = newTemplate)
  }

  extension [Out](opt: Option[Out]) {

    def withLog(message: String)(using Context): Option[Out] = opt match {
      case s: Some[Out] => s
      case None         => report.echo(message); None
    }

    private def withLogVerbose(message: String)(using Context): Option[Out] = opt match {
      case s: Some[Out] => s
      case None         => report.inform(message); None
    }
  }

  extension [Out](t: Try[Out]) {

    def withLog(message: String)(using Context): Option[Out] = t match {
      case Success(value)     => Some(value)
      case Failure(throwable) => report.echo(s"$message; reason:\n\t${throwable.getMessage}"); None
    }
  }
}
