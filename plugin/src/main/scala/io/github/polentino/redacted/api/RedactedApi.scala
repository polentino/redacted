package io.github.polentino.redacted.api

import io.github.polentino.redacted.api.internal.RuntimeApi

/** API that abstracts the redaction process, such that any Scala compiler plugin can invoke its [[process process()]]
  * method and get back the patched (if the validation conditions are met) ```toString``` AST
  *
  * @tparam Api
  *   the [[RuntimeApi]] that actually implements the redaction process
  */
trait RedactedApi[Api <: RuntimeApi] {
  protected val runtime: Api

  def process(tree: runtime.Tree): runtime.Tree
}

object RedactedApi {

  def apply(api: RuntimeApi): RedactedApi[api.type] = new RedactedApi[api.type] {
    override protected val runtime: api.type = api

    override def process(tree: api.Tree): api.Tree = api.process(tree)
  }
}
