package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.*
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.untpd.Modifiers
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.TermName
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.util.Spans.Span
import io.github.polentino.redacted.helpers.AstOps.*

object PluginOps {
  private val TO_STRING_NAME = "toString"

  /** Checks if all preconditions to start the redaction process are met and, if so, executes `withValidTree`
    *
    * @param tree
    *   the tree that will be checked whether it has redacted fields or not
    * @param withValidTree
    *   the action to perform in case there are redacted fields
    * @param Context
    *   implicit param
    * @return
    *   the modified case class `TypeDef` if the conditions were met, or the original one otherwise
    */
  def analyseTree(tree: tpd.TypeDef)(withValidTree: List[String] => tpd.TypeDef)(using Context): tpd.TypeDef =
    val redactedFields = tree.symbol.redactedFields
    if (redactedFields.nonEmpty) {
      withValidTree(redactedFields)
    } else tree
  

  /** Given a case class where `field_A, ..., field_X` are all annotated with ``@redacted``, this method will detect the
    * presence of a `toString` method and patch it in order to not print its sensitive fields
    *
    * @param tree
    *   the typed AST representation of the case class we want to redact
    * @param ref
    *   a reference to the variable `__redactedFields` contained in `tree` companion object
    * @param Context
    *   implicit param
    * @return
    *   the updated AST definition for the case class, with the patched `toString` method
    */
  def patchToStringMethod(tree: tpd.TypeDef, redactedMembers: List[String])(using ctx: Context): tpd.TypeDef = {
    tree.rhs match {
      case template: tpd.Template =>
        val newBody = template.body.map {
          case oldMethodDefinition: tpd.DefDef if oldMethodDefinition.name.toString == TO_STRING_NAME =>
            val className = tree.name.toString
            val memberNames = tree.symbol.primaryConstructor.paramSymss.flatten.toList.map(_.name.toString)
            val string2: List[tpd.Tree] = memberNames.map(m =>
              if (redactedMembers.contains(m)) toConstantLiteral("***")
              else tpd.This(tree.symbol.asClass).select(Names.termName(m))
            )

            def stringify(list: List[tpd.Tree]): tpd.Tree = {
              val plusString = Names.termName("+")
              val comma = toConstantLiteral(",")

              def loop(l: List[tpd.Tree]): List[tpd.Tree] =
                l match {
                  case Nil => Nil
                  case head :: Nil => List(head)
                  case head :: tail => List(head, comma) ++ loop(tail)
                }

              loop(list).fold(toConstantLiteral(className + "(")) { case (l,r) =>
                l.select(plusString).appliedTo(r)
              }.select(plusString).appliedTo(toConstantLiteral(")"))
            }
            
            
            tpd.cpy.DefDef(oldMethodDefinition)(rhs = stringify(string2))
          case otherField => otherField
        }

        val newTemplate = tpd.cpy.Template(template)(body = newBody)
        val newTree = tpd.cpy.TypeDef(tree)(rhs = newTemplate)
        newTree

      case _ => tree
    }
  }
}
