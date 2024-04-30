package io.github.polentino.redacted

import scala.tools.nsc._
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers

final class RedactedPluginComponent(val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "redacted-plugin-component"
  override val runsAfter: List[String] = List("pickler")
//  override val runsRightAfter: Option[String] = Some("typer")

  override def newPhase(prev: Phase): Phase = new StdPhase(prev) {

    override def apply(unit: CompilationUnit): Unit = {
      global.reporter.echo(s"[!] Inspecting Compilation Unit '${unit.toString()}'")
      new Traverser {
        override def traverse(tree: global.Tree): Unit = {
          // Logic that decides whether to transform the tree or not
          if (shouldTransform(tree)) {
            global.reporter.echo(s"\t PATCHING ${tree.symbol.name}")
            val p = tree match {
              case cd: ClassDef =>
                val newImpl = transformTemplate(cd.impl)
                cd.copy(impl = newImpl)
              case c => c
            }
            super.traverse(p)
          } else {
            super.traverse(tree)
          }

          //          val t = tree match {
//            case cd: ClassDef if cd.mods.hasFlag(Flag.CASE) =>
//              global.reporter.echo(s"\t -> inspecting case class '${cd.name}'")
//              val annotations = cd.impl.body.flatMap {
//                case dd: DefDef if dd.name == termNames.CONSTRUCTOR =>
//                  dd.vparamss.flatMap { p =>
//                    p.filter { pp =>
//                      pp.mods.hasAnnotationNamed(TypeName("redacted"))
//                    }
//                  }.map(_.name)
//                case _ => Nil
//              }
//              if (annotations.nonEmpty) {
//                global.reporter.echo(s"\t\t -> annotations: $annotations")
//                val modifiedBody = cd.impl.body.map {
//                  case dd: DefDef if dd.name == TermName("toString") => dd.copy(rhs = Literal(Constant("TEST")))
//                  case other                                         => other
//                }
//                val t = cd.copy(impl = cd.impl.copy(body = modifiedBody))
//                new Transformer {}.transform(t)
//              } else {
//                cd
//              }
//            case c => c
//          }
//          super.traverse(t)
        }
      }.traverse(unit.body)
    }
  }

  private def shouldTransform(tree: global.Tree) = tree match {
    case cd: ClassDef if cd.mods.hasFlag(Flag.CASE) =>
      cd.impl.body.exists {
        case dd: DefDef => hasAnnotation(dd, "redacted")
        case _ => false
      }
    case _ => false
  }

  private def getAnnotations(tree: global.Tree) = {
    tree match {
      case cd: ClassDef if cd.mods.hasFlag(Flag.CASE) =>
//        val annotations = cd.impl.body.flatMap {
//          case dd: DefDef if dd.name == termNames.CONSTRUCTOR =>
//            dd.vparamss.flatMap { p =>
//              p.filter { pp =>
//                pp.mods.hasAnnotationNamed(TypeName("redacted"))
//              }
//            }.map(_.name)
//          case _ => Nil
//        }
//        annotations.nonEmpty

      case _ => Nil
    }
  }

  private def transformTemplate(template: Template): Template = {
    val newBody = template.body.map {
      case dd: DefDef if dd.name == TermName("toString") =>
        global.reporter.echo("\t\t WE HAVE A TOSTRING!")
//        if (hasAnnotation(dd, "redacted")) {
          val newRhs = Literal(Constant("TEST"))
          dd.copy(rhs = newRhs)
//        } else {
//          dd
//        }
      case other => other
    }
    template.copy(body = newBody)
  }

  private def hasAnnotation(dd: DefDef, annotationName: String): Boolean = {
    dd.vparamss.flatten.exists(_.symbol.annotations.exists(_.tree.tpe.typeSymbol.name.toString == annotationName))
  }

  private class MyTransformer(val global: Global) extends Transformer {

    override def transform(tree: Tree): Tree = {
      tree match {
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

          global.reporter.echo(s"\t\t -> annotations: $annotations")
          global.reporter.echo(s"\t\t -> body: $cd.impl.body")
          val modifiedBody = cd.impl.body.map {
            case dd: DefDef if dd.name == TermName("toString") =>
              global.reporter.echo(s"\t -> PATCHED!")
              dd.copy(rhs = Literal(Constant("TEST")))
            case other =>
              global.reporter.echo(s"\t -> SKIPPING ${other.symbol}")
              other
          }
          cd.copy(impl = cd.impl.copy(body = modifiedBody))

        case _ => tree
      }
    }
  }
}
