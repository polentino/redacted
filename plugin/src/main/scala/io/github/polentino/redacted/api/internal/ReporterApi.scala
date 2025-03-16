package io.github.polentino.redacted.api.internal

trait ReporterApi[Api <: RuntimeApi] {
  protected val runtime: Api

  def echo(message: String): Unit
  def warning(pos: runtime.Position, message: String): Unit
}
