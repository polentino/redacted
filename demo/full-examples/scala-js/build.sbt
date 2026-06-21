scalaVersion := "3.7.4" /// or "2.13.18"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin, RedactedPlugin)
  .settings(
    name := "redacted-native",
    redactedVersion := "0.10.0-SNAPSHOT",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )
