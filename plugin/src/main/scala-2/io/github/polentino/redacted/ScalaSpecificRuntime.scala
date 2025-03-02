package io.github.polentino.redacted

import scala.tools.nsc.Global

// this won't work, because we need a hold of `global` type ahead of creation
// final case class ScalaSpecificRuntime[GlobalRef <: Global](global: Global) extends RuntimeApi { .. }
trait ScalaSpecificRuntime[GlobalRef <: Global] extends RuntimeApi {
  val theGlobal: GlobalRef
  override type Tree = theGlobal.Tree
}

object ScalaSpecificRuntime {

  def apply(global: Global): ScalaSpecificRuntime[global.type] = new ScalaSpecificRuntime[global.type ] {
    override val theGlobal: global.type  = global
    import global._
  }
}
