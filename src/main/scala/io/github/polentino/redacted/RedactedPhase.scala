package io.github.polentino.redacted

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.plugins.*
import dotty.tools.dotc.report
import dotty.tools.dotc.transform.Pickler

import io.github.polentino.redacted.helpers.PluginOps._

case class RedactedPhase() extends PluginPhase {

  override val runsAfter: Set[String] = Set(Pickler.name)

  override def phaseName: String = "RedactPhase"

  override def transformTypeDef(tree: tpd.TypeDef)(using Context): tpd.Tree = {
    val finalTree = analyzeTree(tree) { fieldsToRedact =>
      report.inform(s"\"${tree.name}\" has the following redacted fields: ${fieldsToRedact}", ctx.owner.srcPos)
      patchTree(tree, fieldsToRedact)
    }

    super.transformTypeDef(finalTree)
  }
}
