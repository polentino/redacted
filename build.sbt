ThisBuild / scalaVersion := "3.1.3"
ThisBuild / versionScheme := Some("early-semver")

// interesting; bumping scalatest / scalacheck, makes 3.1.x and 3.2.x compilation to fail
val scalaTestVersion = "3.2.20"
val scalaCheckVersion = "3.2.17.0"
val scalaCheckNativeVersion = "3.2.19.0"

// versions overrides needed to address vulnerabilities
val protobufJavaVersion = "4.35.1"
val jacksonCoreVersion = "2.22.0"

// Scala versions for the compiler plugin
val compilerPluginScalaVersions = List(
  "2.12.21",
  "2.13.18",
  "3.1.3",
  "3.2.2",
  "3.3.0",
  "3.3.1",
  "3.3.3",
  "3.3.4",
  "3.3.5",
  "3.3.6",
  "3.3.7",
  "3.3.8",
  "3.4.3",
  "3.5.2",
  "3.6.4",
  "3.7.4",
  "3.8.4"
)

// Scala versions used for Scala.js / Scala Native (supported by both toolchains)
val platformScalaVersions = List(
  "2.12.21",
  "2.13.18",
  "3.3.0"
)

// Scala versions for the annotation library. TODO: perhaps merge with `platformScalaVersions`
val libraryScalaVersions = List(
  "2.12.21",
  "2.13.18",
  "3.3.0"
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
  .aggregate(
    redactedLibrary.jvm,
    redactedLibrary.js,
    redactedLibrary.native,
    redactedCompilerPlugin,
    redactedTests.jvm,
    redactedTests.js,
    redactedTests.native
  )

val scalafixSettings = Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

val dependenciesOverride = Seq(
  "com.google.protobuf"        % "protobuf-java" % protobufJavaVersion,
  "com.fasterxml.jackson.core" % "jackson-core"  % jacksonCoreVersion
)

val compilerPluginSettings = scalafixSettings ++ Seq(
  Test / skip := true,
  crossScalaVersions := compilerPluginScalaVersions,
  dependencyOverrides ++= dependenciesOverride
)

val librarySettings = scalafixSettings ++ Seq(
  Test / skip := true,
  crossScalaVersions := libraryScalaVersions,
  dependencyOverrides ++= dependenciesOverride
)

def redactedPluginScalacOptions = Def.task {
  val addScala2Plugin = "-Xplugin-require:redacted-plugin"
  val jar = (redactedCompilerPlugin / Compile / packageBin).value
  val addScala3Plugin = "-Xplugin:" + jar.getAbsolutePath
  val dummy = "-Jdummy=" + jar.lastModified
  Seq(addScala2Plugin, addScala3Plugin, dummy)
}

lazy val redactedLibrary = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("library"))
  .settings(name := "redacted")
  .settings(librarySettings)
  .jsSettings(crossScalaVersions := platformScalaVersions)
  .nativeSettings(crossScalaVersions := platformScalaVersions)

lazy val redactedCompilerPlugin = (project in file("plugin"))
  .settings(name := "redacted-plugin")
  .settings(
    compilerPluginSettings,
    crossTarget :=
      target.value / s"scala-${scalaVersion.value}", // workaround for https://github.com/sbt/sbt/issues/5097
    crossVersion := CrossVersion.full,
    libraryDependencies +=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
        case Some((2, _)) => "org.scala-lang"  % "scala-compiler"  % scalaVersion.value
        case v            => throw new Exception(s"Scala version $v not recognised")
      })
  )

lazy val redactedTests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("tests"))
  .dependsOn(redactedLibrary)
  .settings(name := "redacted-tests")
  .settings(scalafixSettings)
  .settings(
    publish / skip := true,
    crossScalaVersions := compilerPluginScalaVersions,
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
    Test / scalacOptions ++= redactedPluginScalacOptions.value
  )
  .jvmSettings(
    libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-17" % scalaCheckVersion % Test
  )
  .jsSettings(
    crossScalaVersions := platformScalaVersions,
    libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-18" % scalaCheckNativeVersion % Test
  )
  .nativeSettings(
    crossScalaVersions := platformScalaVersions,
    libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-18" % scalaCheckNativeVersion % Test
  )

lazy val site = (project in file("redacted-docs"))
  .enablePlugins(DocusaurPlugin)
  .settings(
    name := "redacted-docs",
    publish / skip := true,
    docusaurDir := (ThisBuild / baseDirectory).value / "redacted-docs",
    docusaurBuildDir := docusaurDir.value / "build",
    gitHubPagesOrgName := "polentino",
    gitHubPagesRepoName := "redacted"
  )

addCommandAlias("testAll", "; clean; +test")
addCommandAlias("fmt", "; scalafix; scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "; scalafmtCheckAll ; scalafmtSbtCheck")
addCommandAlias("crossReleaseAll", "; clean; +publishSigned; sonaUpload; sonaRelease")
