ThisBuild / version := "0.2.1-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"

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
    name := "redacted",
    publish / skip := true
  )
  .aggregate(library, plugin)

val scalafixSettings = Seq(
  scalafixDependencies += "com.liancheng" %% "organize-imports" % "0.6.0",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val library = (project in file("library"))
  .settings(name := "redacted-annotation-library")
  .settings(scalafixSettings)

lazy val plugin = (project in file("plugin"))
  .settings(name := "redacted-compiler-plugin")
  .settings(scalafixSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "provided",
      "org.scalatest"  %% "scalatest"       % "3.2.17"           % "test"
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
