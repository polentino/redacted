import xerial.sbt.Sonatype.GitHubHosting

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/polentino/redacted"),
    "scm:git@github.com:polentino/redacted.git"
  )
)
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / licenses := List("WTFPL" -> url("http://www.wtfpl.net/"))
ThisBuild / homepage := Some(url("https://github.com/polentino/redacted"))
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("polentino", "redacted", "polentino911@gmail.com"))
ThisBuild / developers := List(
  Developer("polentino", "Diego Casella", "polentino911@gmail.com", url("https://linkedin.com/in/diegocasella"))
)

usePgpKeyHex("8326E86A8331A3C871E392795045B78015B14EE0")