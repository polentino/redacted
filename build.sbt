
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

val crossCompileSettings = scalafixSettings ++ Seq(
  Test / skip := true,
  crossTarget := target.value / s"scala-${scalaVersion.value}", // workaround for https://github.com/sbt/sbt/issues/5097
  crossVersion := CrossVersion.full,
  crossScalaVersions := supportedScalaVersions,
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
  .settings(crossCompileSettings)
  .jsSettings(crossScalaVersions := platformScalaVersions)
  .nativeSettings(crossScalaVersions := platformScalaVersions)

lazy val redactedCompilerPlugin = (project in file("plugin"))
  .settings(name := "redacted-plugin")
  .settings(
    crossCompileSettings,
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
    crossScalaVersions := supportedScalaVersions,
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
