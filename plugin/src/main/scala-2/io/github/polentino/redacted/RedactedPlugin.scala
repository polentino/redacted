package io.github.polentino.redacted

import scala.tools.nsc._
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

final class RedactedPlugin(override val global: Global) extends Plugin {

  override val name: String = "redacted-plugin"

  override val description: String = "Plugin to prevent leaking sensitive data when logging case classes"

  override val components: List[PluginComponent] = List(new RedactedPluginComponent(global))
}
