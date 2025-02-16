package io.github.polentino.redacted

import dotty.tools.dotc.plugins.*

import io.github.polentino.redacted.phases.*

class RedactedPlugin extends StandardPlugin {
  override def init(options: List[String]): List[PluginPhase] = List(PatchToString())

  override def name: String = "redacted-plugin"

  override def description: String = "Plugin to prevent leaking sensitive data when logging case classes"
}
