scalaVersion := "3.7.4" /// or "3.9.0"

import scala.scalanative.build._

lazy val root = (project in file("."))
  .enablePlugins(ScalaNativePlugin, RedactedPlugin)
  .settings(
    name := "redacted-native",
    redactedVersion := "0.10.0",
    nativeConfig ~= {
      _.withMode(Mode.releaseFast)
        .withLTO(LTO.thin)
        .withGC(GC.immix)
    }
  )
