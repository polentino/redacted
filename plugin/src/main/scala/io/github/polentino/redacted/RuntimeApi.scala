package io.github.polentino.redacted

trait RuntimeApi {
  type Tree
  type Position

  def process(tree: Tree): Tree = tree
}
