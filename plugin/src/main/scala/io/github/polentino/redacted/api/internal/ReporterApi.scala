package io.github.polentino.redacted.api.internal

/** Small façade API to log messages to console.
  * @tparam Api
  *   an instance of [[RuntimeApi]], needed to share the same [[ReporterApi#Position]] type.
  */
trait ReporterApi[Api <: RuntimeApi] {
  protected val runtime: Api

  /** Equivalent of log.info(...)
    * @param message
    *   the message to print
    */
  def echo(message: String): Unit

  /** Equivalent of log.warn(...)
    * @param message
    *   the message to print
    */
  def warning(message: String, pos: runtime.Position): Unit
}
