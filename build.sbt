ThisBuild / scalaVersion := "3.1.3"

val organizeImportVersion = "0.6.0"
// interesting; bumping scalatest / scalacheck, makes 3.1.x and 3.2.x compilation to fail
val scalaTestVersion = "3.2.17"
val scalaCheckVersion = "3.2.18.0"

// all LTS versions & latest minor ones
val supportedScalaVersions = List(
  "2.12.20",
  "2.13.16",
  "3.1.3",
  "3.2.2",
  "3.3.0",
  "3.3.1",
  "3.3.3",
  "3.3.4",
  "3.3.5",
  "3.4.3",
  "3.5.2",
  "3.6.3"
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
    libraryDependencies += (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
      case Some((2, _)) => "org.scala-lang"  % "scala-compiler"  % scalaVersion.value
      case v            => throw new Exception(s"Scala version $v not recognised")
    })
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
      val addScala2Plugin = "-Xplugin-require:redacted-plugin"
      val addScala3Plugin = "-Xplugin:" + jar.getAbsolutePath
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addScala2Plugin, addScala3Plugin, dummy)
    }
  )

addCommandAlias("testAll", "; clean; +test")
addCommandAlias("fmt", "; scalafix; scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "; scalafmtCheckAll ; scalafmtSbtCheck")
addCommandAlias("crossReleaseAll", "; clean; +publishSigned; sonatypeBundleRelease")
