package io.github.polentino.redacted

trait ReporterApi {
  type Pos

  def echo(message: String): Unit
  def warning(pos: Pos, message: String): Unit
}
