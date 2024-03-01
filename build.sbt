ThisBuild / version := "0.2.2.1-SNAPSHOT"
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
  .aggregate(redactedLibrary, redactedCompilerPlugin)

val scalafixSettings = Seq(
  scalafixDependencies += "com.liancheng" %% "organize-imports" % "0.6.0",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val redactedLibrary = (project in file("library"))
  .settings(name := "redacted")
  .settings(scalafixSettings)

lazy val redactedCompilerPlugin = (project in file("plugin"))
  .settings(name := "redacted-compiler-plugin")
  .settings(scalafixSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % Provided,
      "org.scalatest"  %% "scalatest"       % "3.2.17" % "test"
    ),
    Compile / managedSources ++= {
      val baseDir = baseDirectory.value / ".." / "library" / "src" / "main" / "scala"
      Seq(baseDir / "io" / "github" / "polentino" / "redacted" / "redacted.scala")
    },
    Test / scalacOptions ++= {
      val jar = (Compile / packageBin).value
      Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll test:scalafmtAll; scalafixAll")
