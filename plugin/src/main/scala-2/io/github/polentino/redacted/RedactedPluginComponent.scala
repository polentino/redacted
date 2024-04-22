package io.github.polentino.redacted

import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

final class RedactedPluginComponent(val global: Global) extends PluginComponent with TypingTransformers {
  import global._
  override val phaseName: String = "redacted-plugin-component"
  override val runsAfter: List[String] = List("parser")

  override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = ()
  }
}
