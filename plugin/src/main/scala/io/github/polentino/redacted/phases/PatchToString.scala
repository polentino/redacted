package io.github.polentino.redacted.phases

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.transform.Pickler

import io.github.polentino.redacted.helpers.PluginOps.*

final case class PatchToString() extends PluginPhase {

  override val runsAfter: Set[String] = Set(Pickler.name)

  override def phaseName: String = PatchToString.name

  override def transformTypeDef(tree: tpd.TypeDef)(using Context): tpd.Tree = {
    analyseTree(tree) { ref =>
      patchToStringMethod(tree, ref)
    }
  }
}

object PatchToString {
  final val name: String = "PatchToString"
}
