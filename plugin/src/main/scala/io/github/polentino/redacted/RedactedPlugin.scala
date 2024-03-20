package io.github.polentino.redacted

import dotty.tools.dotc.plugins.*

import io.github.polentino.redacted.phases._

class RedactedPlugin extends StandardPlugin {
  override def init(options: List[String]): List[PluginPhase] = List(PatchToString())

  override def name: String = "Redacted"

  override def description: String = "Plugin to prevent leaking sensitive data when logging case classes"
}
