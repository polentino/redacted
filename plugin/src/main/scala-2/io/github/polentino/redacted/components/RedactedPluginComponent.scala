package io.github.polentino.redacted.components

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform

import io.github.polentino.redacted.api.RedactedApi
import io.github.polentino.redacted.runtime.Scala2Runtime

class RedactedPluginComponent(val global: Global) extends PluginComponent with Transform {

  override val phaseName: String = "patch-tostring-component"

  override val runsAfter: List[String] = List("parser")

  import global._
  val runtimeApi: Scala2Runtime[global.type] = Scala2Runtime(global)
  val redactedApi: RedactedApi[runtimeApi.type] = RedactedApi(runtimeApi)

  override protected def newTransformer(unit: CompilationUnit): Transformer = new RedactedToStringTransformer()

  private class RedactedToStringTransformer() extends Transformer {

    override def transform(tree: Tree): Tree =
      redactedApi.process(super.transform(tree))
  }
}
