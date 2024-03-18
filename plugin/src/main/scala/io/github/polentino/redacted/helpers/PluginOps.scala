package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.{tpd, *}
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.untpd.Modifiers
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.TermName
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.util.Spans.Span
import io.github.polentino.redacted.helpers.AstOps.*

import scala.util.{Failure, Success, Try}

object PluginOps {
  private val TO_STRING_NAME = "toString"

  def validate(tree: tpd.TypeDef)(using Context): Option[tpd.TypeDef] = for {
    caseClassType <- validateTypeDef(tree)
      .withLogVerbose(s"${tree.name} isn't a case class")
    _ <- getRedactedFields(caseClassType)
      .withLogVerbose(s"${tree.name} doesn't contain `@redacted` fields")
  } yield caseClassType

  private def validateTypeDef(tree: tpd.TypeDef)(using Context) = Option.when(tree.isCaseClass)(tree)

  private def getRedactedFields(tree: tpd.TypeDef)(using Context): Option[List[String]] = {
    val redactedFields = tree.symbol.redactedFields
    Option.when(redactedFields.nonEmpty)(redactedFields)
  }

  def getTreeTemplate(tree: tpd.TypeDef)(using Context): Option[tpd.Template] = tree.rhs match {
    case template: tpd.Template => Some(template)
    case _                      => None
  }

  def createToStringBody(tree: tpd.TypeDef)(using Context): Try[tpd.Tree] = Try {
    val className = tree.name.toString
    val memberNames = tree.symbol.primaryConstructor.paramSymss.flatten
    val annotationSymbol = redactedSymbol
    val classPrefix = (className + "(").toConstantLiteral
    val classSuffix = ")".toConstantLiteral
    val commaSymbol = ",".toConstantLiteral
    val asterisksSymbol = "***".toConstantLiteral
    val concatOperator = Names.termName("+")

    val fragments: List[tpd.Tree] = memberNames.map(m =>
      if (m.annotations.exists(_.matches(annotationSymbol))) asterisksSymbol
      else tpd.Select(tpd.This(tree.symbol.asClass), m.name))

    def buildToStringTree(fragments: List[tpd.Tree]): tpd.Tree = {

      def concatAll(l: List[tpd.Tree]): List[tpd.Tree] = l match {
        case Nil          => Nil
        case head :: Nil  => List(head)
        case head :: tail => List(head, commaSymbol) ++ concatAll(tail)
      }

      val res = concatAll(fragments).fold(classPrefix) { case (l, r) =>
        tpd.Apply(tpd.Select(l, concatOperator), r :: Nil)
      }
      tpd.Apply(tpd.Select(res, concatOperator), classSuffix :: Nil)
    }

    buildToStringTree(fragments)
  }

  def patchToString(template: tpd.Template, toStringTree: tpd.Tree)(using Context): Try[tpd.Template] = Try {
    val newBody = template.body.map {
      case oldMethodDefinition: tpd.DefDef if oldMethodDefinition.name.toString == TO_STRING_NAME =>
        tpd.cpy.DefDef(oldMethodDefinition)(rhs = toStringTree)
      case otherField => otherField
    }

    tpd.cpy.Template(template)(body = newBody)
  }

  def patchTypeDef(tree: tpd.TypeDef, template: tpd.Template)(using Context): Try[tpd.TypeDef] = Try {
    tpd.cpy.TypeDef(tree)(rhs = template)
  }

  extension [Out](opt: Option[Out]) {

    def withLog(message: String)(using Context): Option[Out] =
      opt match {
        case s: Some[Out] => s
        case None         => report.echo(message); None
      }

    private def withLogVerbose(message: String)(using Context): Option[Out] =
      opt match {
        case s: Some[Out] => s
        case None         => report.inform(message); None
      }
  }

  extension [Out](t: Try[Out]) {

    def withLog(message: String)(using Context): Option[Out] =
      t match {
        case Success(value)     => Some(value)
        case Failure(throwable) => report.echo(s"$message; reason:\n\t${throwable.getMessage}"); None
      }
  }
}
