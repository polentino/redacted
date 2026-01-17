ThisBuild / organization := "io.github.polentino"
ThisBuild / organizationName := "io.github.polentino"
ThisBuild / organizationHomepage := Some(url("https://github.com/polentino/redacted"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/polentino/redacted"),
    "scm:git@github.com:polentino/redacted.git"
  )
)

ThisBuild / developers := List(
  Developer("polentino", "Diego Casella", "polentino911@gmail.com", url("https://linkedin.com/in/diegocasella"))
)

ThisBuild / description :=
  "Scala library and compiler plugin that prevent inadvertent leakage of sensitive fields in `case classes` (such as credentials, personal data, and other confidential information)."
ThisBuild / licenses := List("WTFPL" -> url("http://www.wtfpl.net/"))
ThisBuild / homepage := Some(url("https://polentino.github.io/redacted/"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

usePgpKeyHex("5A167E17FB1B86685ECFD29A2F7D08508EB255BF")
