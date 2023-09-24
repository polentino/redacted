package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.{tpd, *}
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.plugins.PluginPhase
import io.github.polentino.redacted.redacted

object PluginOps {
  private val REDACTED_CLASS = classOf[redacted].getCanonicalName
  private val TO_REDACTED_STRING = "io.github.polentino.redacted.helpers.toRedactedString"

  /**
   * Checks if all preconditions to start the redaction process are met and, if so, executes `withValidTree`
   *
   * @param tree          the tree to be checked
   * @param withValidTree the function to be executed if the preconditions are valid
   * @param Context       implicit param
   * @return the modified case class `TypeDef` if the preconditions were met, or the original one otherwise
   */
  def analyzeTree(tree: tpd.TypeDef)(withValidTree: List[String] => tpd.TypeDef)(using Context): tpd.TypeDef = {
    if (tree.symbol.is(Flags.CaseClass)) {
      val redactedYpe = Symbols.requiredClass(REDACTED_CLASS)
      val fieldsToRedact = tree.symbol.primaryConstructor.paramSymss.flatten.collect {
        case s if s.annotations.exists(_.matches(redactedYpe)) => s.name.toString
      }

      if (fieldsToRedact.nonEmpty) withValidTree(fieldsToRedact)
      else tree

    } else tree
  }

  /**
   * Given a case class where `field_A, ..., field_X` are all annotated with ``@redacted``, this method will detect the
   * presence of a `toString` method and patch it in order to not print its sensitive fields
   *
   * @param tree                  the typed AST representation of the case class we want to redact
   * @param fieldsWithAnnotations the specific fields we want to redact
   * @param Context               implicit param
   * @return the updated AST definition for the case class, with the patched `toString` method
   */
  def patchTree(tree: tpd.TypeDef, fieldsWithAnnotations: List[String])(using Context): tpd.TypeDef = {
    tree.rhs match {
      case template: tpd.Template =>
        val newBody = template.body.map {
          case oldMethodDefinition: tpd.DefDef if oldMethodDefinition.name.toString == "toString" =>
            patchToString(oldMethodDefinition, fieldsWithAnnotations)

          case otherField => otherField
        }

        val newTemplate = tpd.cpy.Template(template)(body = newBody)
        val newTree = tpd.cpy.TypeDef(tree)(rhs = newTemplate)
        newTree

      case _ => tree
    }
  }

  /**
   * Given a case class where `field_A, ..., field_X` are all annotated with ``@redacted``,
   * this method changes the typed AST definition of `toString` from
   * <blockquote><pre>
   * def toString(): String = scala.runtime.ScalaRunTime._toString(this)
   * </pre></blockquote>
   * to
   * <blockquote><pre>
   * def toString(): String = io.github.polentino.redacted.helpers.toRedactedString(this, Seq(field_A, ..., field_X))
   * </pre></blockquote>
   *
   * @param oldToStringDefinition the AST of the old `toString` method definition
   * @param fieldsToRedact        a list of field names that needs to be redacted from the owning case class
   * @param Context               implicit param
   * @return the new AST method definition for toString that will redact the sensitive fields
   */
  private def patchToString(oldToStringDefinition: tpd.DefDef, fieldsToRedact: List[String])(using Context): tpd.DefDef = {
    // Seq[String](field_A, ... , field_X)
    val seqArgs = tpd.SeqLiteral(fieldsToRedact.map(_.toConstantLiteral), tpd.TypeTree(defn.StringType))

    // toRedactedString(this, Seq[String](field_A, ... , field_X))
    val toRedactedStringSymbol: Symbol = requiredMethod(TO_REDACTED_STRING)
    val thisRef: tpd.This = tpd.This(oldToStringDefinition.symbol.owner.asClass)
    val newMethodBody = tpd.Apply(tpd.ref(toRedactedStringSymbol), List(thisRef, seqArgs))

    // def toString = toRedactedString(this, Seq[String](field_A, ... , field_X))
    tpd.cpy.DefDef(oldToStringDefinition)(rhs = newMethodBody)
  }

  private implicit class StringOps(s: String) extends AnyVal {
    def toConstantLiteral(using Context): tpd.Literal = tpd.Literal(Constant(s))
  }
}
