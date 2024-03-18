package io.github.polentino.redacted.phases

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Names
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.report
import dotty.tools.dotc.transform.Pickler
import io.github.polentino.redacted.helpers.PluginOps.*
import io.github.polentino.redacted.helpers.AstOps.*

import scala.util.Try

final case class PatchToString() extends PluginPhase {

  override val runsAfter: Set[String] = Set(Pickler.name)

  override def phaseName: String = PatchToString.name

  override def transformTypeDef(tree: tpd.TypeDef)(using Context): tpd.Tree = validate(tree) match {
    case None => tree
    case Some(validatedTree) =>
      val maybeNewTypeDef = for {

        template <- getTreeTemplate(validatedTree)
          .withLog(s"can't extract proper `tpd.Template` from ${tree.name}")

        toStringBody <- createToStringBody(validatedTree, template)
          .withLog(s"couldn't create proper `toString()` body")

        newTemplate <- patchToString(template, toStringBody)
          .withLog(s"couldn't patch `toString()` body into ${tree.name} template")

        result <- patchTypeDef(validatedTree, newTemplate)
          .withLog(s"couldn't patch ${tree.name} template into ${tree.name} typedef")
      } yield result

      maybeNewTypeDef match
        case Some(newTypeDef) => newTypeDef
        case None =>
          report.warning(
            s"""
                 |Dang, couldn't patch properly ${tree.name} :(
                 |If you believe this is an error: please report the issue, along with a minimum reproducible example, at
                 |the following link: https://github.com/polentino/redacted/issues/new
                 |
                 |Thank you 🙏
                 |""".stripMargin,
            tree.srcPos
          )
          tree
  }
}

object PatchToString {
  final val name: String = "PatchToString"
}
