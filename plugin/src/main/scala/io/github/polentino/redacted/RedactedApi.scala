package io.github.polentino.redacted

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