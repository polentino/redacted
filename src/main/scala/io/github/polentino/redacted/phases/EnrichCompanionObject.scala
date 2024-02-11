package io.github.polentino.redacted.phases

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.report
import dotty.tools.dotc.transform.Pickler

import io.github.polentino.redacted.helpers.PluginOps.*

final case class EnrichCompanionObject() extends PluginPhase {

  override val runsAfter: Set[String] = Set(Pickler.name)

  override def phaseName: String = EnrichCompanionObject.name

  override def transformTypeDef(tree: tpd.TypeDef)(using Context): tpd.Tree = {
    whenRedactedCompanionObject(tree) { fieldsToRedact =>
      report.inform(s"\"${tree.name.toString}\" has the following redacted fields: ${fieldsToRedact}", tree.srcPos)
      tree.rhs match {
        case template: tpd.Template =>
          val redactedFieldsVal = createRedactedFieldsValDef(createFieldsSequence(fieldsToRedact), tree.symbol.asClass)
          val newTreeTemplate = tpd.cpy.Template(template)(body = redactedFieldsVal :: template.body)
          tpd.cpy.TypeDef(tree)(tree.name, newTreeTemplate)

        case _ => tree
      }
    }
  }
}

object EnrichCompanionObject {
  final val name: String = "EnrichCompanionObject"
}
