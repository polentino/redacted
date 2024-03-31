ThisBuild / scalaVersion := "3.1.3"

val organizeImportVersion = "0.6.0"
val scalaTestVersion = "3.2.17"
val scalaCheckVersion = "3.2.17.0"

// all LTS versions & latest minor ones
val supportedScalaVersions = List(
  "3.1.3",
  "3.2.2",
  "3.3.0",
  "3.3.1",
  "3.3.3",
  "3.4.0"
)

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
        url("https://linkedin.com/in/diegocasella")
      )
    )
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "redacted-root",
    crossScalaVersions := Nil,
    test / skip := true,
    publish / skip := true
  )
  .aggregate(redactedLibrary, redactedCompilerPlugin, redactedTests)

val scalafixSettings = Seq(
  scalafixDependencies += "com.liancheng" %% "organize-imports" % organizeImportVersion,
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

val crossCompileSettings = scalafixSettings ++ Seq(
  Test / skip := true,
  crossTarget := target.value / s"scala-${scalaVersion.value}", // workaround for https://github.com/sbt/sbt/issues/5097
  crossVersion := CrossVersion.full,
  crossScalaVersions := supportedScalaVersions
)

lazy val redactedLibrary = (project in file("library"))
  .settings(name := "redacted")
  .settings(crossCompileSettings)

lazy val redactedCompilerPlugin = (project in file("plugin"))
  .settings(name := "redacted-plugin")
  .settings(
    crossCompileSettings,
    libraryDependencies += "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
  )

lazy val redactedTests = (project in file("tests"))
  .dependsOn(redactedLibrary)
  .settings(name := "redacted-tests")
  .settings(scalafixSettings)
  .settings(
    publish / skip := true,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      "org.scalatest"     %% "scalatest"       % scalaTestVersion  % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % scalaCheckVersion % Test
    ),
    Test / scalacOptions ++= {
      val jar = (redactedCompilerPlugin / Compile / packageBin).value
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )

addCommandAlias("testAll", "; clean; +test")
addCommandAlias("fmt", "; scalafix; scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "; scalafmtCheckAll ; scalafmtSbtCheck")
addCommandAlias("crossReleaseAll", "; clean; +publishSigned; sonatypeBundleRelease")
