ThisBuild / version := "0.2.2.4-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"

ThisBuild / publishMavenStyle := true
ThisBuild / crossPaths := false
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / publishTo := Some(
  "GitHub Package Registry" at "https://maven.pkg.github.com/polentino/redacted"
)
ThisBuild / credentials +=
  Credentials(
    "GitHub Package Registry",
    "maven.pkg.github.com",
    "polentino",
    sys.env.getOrElse("GITHUB_TOKEN", "???"))

inThisBuild(
  List(
    organization := "io.github.polentino",
    homepage := Some(url("https://github.com/polentino/redacted")),
    licenses := List(
      "WTFPL" -> url("http://www.wtfpl.net/")
    ),
    developers := List(
      Developer(
        "polentino",
        "Diego Casella",
        "polentino911@gmail.com",
        url("https://be.linkedin.com/in/diegocasella")
      )
    )
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "redacted-root",
    publish / skip := true
  )
  .aggregate(redactedLibrary, redactedCompilerPlugin, redactedTests)

val scalafixSettings = Seq(
  scalafixDependencies += "com.liancheng" %% "organize-imports" % "0.6.0",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val redactedLibrary = (project in file("library"))
  .settings(name := "redacted")
  .settings(
    scalafixSettings,
    Test / skip := true
  )

lazy val redactedCompilerPlugin = (project in file("plugin"))
  .dependsOn(redactedLibrary)
  .settings(name := "redacted-plugin")
  .settings(scalafixSettings)
  .settings(
    Test / skip := true,
    assembly / assemblyJarName := {
      val assemblyJarFile = (Compile / Keys.`package`).value
      assemblyJarFile.getName
    },
    libraryDependencies += "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
  )

lazy val redactedTests = (project in file("tests"))
  .dependsOn(redactedLibrary)
  .settings(name := "redacted-tests")
  .settings(scalafixSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.17" % Test),
    scalacOptions ++= {
      val jar = (redactedCompilerPlugin / assembly).value
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )

addCommandAlias("fmt", "; scalafmtAll ; scalafmtSbt") // todo scalafix ?
addCommandAlias("fmtCheck", "; scalafmtCheckAll ; scalafmtSbtCheck")
