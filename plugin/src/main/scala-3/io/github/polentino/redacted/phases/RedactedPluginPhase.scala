package io.github.polentino.redacted.phases

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.transform.Pickler

import io.github.polentino.redacted.api.RedactedApi
import io.github.polentino.redacted.runtime.Scala3Runtime

final case class RedactedPluginPhase() extends PluginPhase {

  override val runsAfter: Set[String] = Set(Pickler.name)

  override def phaseName: String = RedactedPluginPhase.name

  override def transformDefDef(tree: tpd.DefDef)(using ctx: Context): tpd.Tree = {
    val runtimeApi: Scala3Runtime = Scala3Runtime.create
    val redactedApi: RedactedApi[runtimeApi.type] = RedactedApi(runtimeApi)
    redactedApi.process(super.transformDefDef(tree))
  }
}

object RedactedPluginPhase {
  final val name: String = "patch-tostring-phase"
}
