ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

lazy val root = (project in file("."))
  .settings(
    name := "redacted",
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "provided",
      "org.scalatest" %% "scalatest" % "3.2.17" % "test"
    ),
    Test / scalacOptions ++= {
      val jar = (Compile / packageBin).value
      Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
    },
    //    ,
    //    idePackagePrefix := Some("io.github.polentino")
  )
