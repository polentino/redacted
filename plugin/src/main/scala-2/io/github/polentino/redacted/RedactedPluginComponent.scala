package io.github.polentino.redacted

import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

final class RedactedPluginComponent(val global: Global) extends PluginComponent with TypingTransformers {
  import global._
  override val phaseName: String = "redacted-plugin-component"
  override val runsAfter: List[String] = List("parser")
  override val runsRightAfter: Option[String] = Some("parser")

  override def newPhase(prev: Phase): Phase = new StdPhase(prev) {

    override def apply(unit: CompilationUnit): Unit = {
      global.reporter.echo(s"[!] Inspecting Compilation Unit '${unit.toString()}'")
      new Traverser {
        override def traverse(tree: global.Tree): Unit = {
          val t = tree match {
            case cd: ClassDef if cd.mods.hasFlag(Flag.CASE) =>
              global.reporter.echo(s"\t -> inspecting case class '${cd.name}'")
              val annotations = cd.impl.body.flatMap {
                case dd: DefDef if dd.name == termNames.CONSTRUCTOR =>
                  dd.vparamss.flatMap { p =>
                    p.filter { pp =>
                      pp.mods.hasAnnotationNamed(TypeName("redacted"))
                    }
                  }.map(_.name)
                case _ => Nil
              }
              if (annotations.nonEmpty) {
                global.reporter.echo(s"\t\t -> annotations: $annotations")
              }
              cd
            case c => c
          }
          super.traverse(t)
        }
      }.traverse(unit.body)
    }
  }
}
