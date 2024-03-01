package io.github.polentino.redacted.helpers

import dotty.tools.dotc.*
import dotty.tools.dotc.ast.*
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.TermName
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.*
import dotty.tools.dotc.util.Spans.Span

import io.github.polentino.redacted.helpers.AstOps.*

object PluginOps {
  private val TO_REDACTED_STRING = "io.github.polentino.redacted.helpers.toRedactedString"
  private val REDACTED_FIELDS_VARIABLE = "__redactedFields"
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
  def analyseTree(tree: tpd.TypeDef)(withValidTree: Types.TermRef => tpd.TypeDef)(using Context): tpd.TypeDef =
    if (tree.symbol.redactedFields.nonEmpty) {
      val valueRef = tree.symbol.linkedClass.requiredValueRef(Names.termName(REDACTED_FIELDS_VARIABLE))
      withValidTree(valueRef)
    } else tree

  /** Checks if all preconditions to start the enrichment process are met and, if so, executes `withValidTree`
    *
    * @param tree
    *   the tree that will be checked whether it has a companion clas with redacted fields or not
    * @param withValidTree
    *   the action to perform in case there are redacted fields
    * @param Context
    *   implicit param
    * @return
    *   the modified object `TypeDef` if the conditions were met, or the original one otherwise
    */
  def whenRedactedCompanionObject(tree: tpd.TypeDef)(withValidTree: List[String] => tpd.TypeDef)(using
    Context
  ): tpd.TypeDef =
    if (tree.isCompanionObject && tree.hasCompanionCaseClass) {
      val fieldsToRedact = tree.symbol.linkedClass.redactedFields
      if (fieldsToRedact.nonEmpty) withValidTree(fieldsToRedact) else tree
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
  def patchToStringMethod(tree: tpd.TypeDef, ref: Types.TermRef)(using ctx: Context): tpd.TypeDef = {
    tree.rhs match {
      case template: tpd.Template =>
        val newBody = template.body.map {
          case oldMethodDefinition: tpd.DefDef if oldMethodDefinition.name.toString == TO_STRING_NAME =>
            patchToString(oldMethodDefinition, tpd.ref(ref))

          case otherField => otherField
        }

        val newTemplate = tpd.cpy.Template(template)(body = newBody)
        val newTree = tpd.cpy.TypeDef(tree)(rhs = newTemplate)
        newTree

      case _ => tree
    }
  }

  /** Given a list of strings, it create a TPD representation of a sequence of those
    *
    * @param fieldsToRedact
    *   the names of the fields that need to be redacted
    * @param Context
    *   implicit param
    * @return
    *   a `tpd.SeqLiteral` representation of aforementioned list
    */
  def createFieldsSequence(fieldsToRedact: List[String])(using Context): tpd.SeqLiteral =
    tpd.SeqLiteral(fieldsToRedact.map(_.toConstantLiteral), tpd.TypeTree(defn.StringType))

  /** Creates a TPD representation of a value, for the `tree` passed as parameter, with a given owner
    *
    * @param rhs
    *   the right hand side `tpd.Tree` of the val assingment
    * @param owner
    *   the owner of the val
    * @param Context
    *   implicit param
    * @return
    *   the complete definition of the value
    */
  def createRedactedFieldsValDef(rhs: tpd.Tree, owner: Symbol)(using Context): tpd.ValDef = {
    val redactedFieldsSymbol = newSymbol[TermName](
      owner,
      Names.termName(REDACTED_FIELDS_VARIABLE),
      Flags.Final | Flags.OuterAccessor | Flags.Synthetic,
      defn.SeqType.appliedTo(defn.StringType),
      owner,
      owner.coord
    ).entered

    tpd.ValDef(redactedFieldsSymbol, rhs).withSpan(Span(0, 0, 0))
  }

  private def patchToString(oldToStringDefinition: tpd.DefDef, seqArgs: tpd.Tree)(using Context): tpd.DefDef = {
    // toRedactedString(this, fqdn.CompanionObject.__redactedFields)
    val pkgSymbol: Symbol = requiredPackage("io.github.polentino.redacted.helpers")
    val toRedactedStringSymbol: Symbol = pkgSymbol.requiredMethod("toRedactedString")
    val thisRef: tpd.This = tpd.This(oldToStringDefinition.symbol.owner.asClass)
    val newMethodBody = tpd.Apply(tpd.ref(toRedactedStringSymbol), List(thisRef, seqArgs))

    // def toString = toRedactedString(this, fqdn.CompanionObject.__redactedFields)
    tpd.cpy.DefDef(oldToStringDefinition)(rhs = newMethodBody)
  }
}
