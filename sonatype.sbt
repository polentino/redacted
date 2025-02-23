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
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("polentino", "redacted", "polentino911@gmail.com"))
ThisBuild / developers := List(
  Developer("polentino", "Diego Casella", "polentino911@gmail.com", url("https://linkedin.com/in/diegocasella"))
)

usePgpKeyHex("5A167E17FB1B86685ECFD29A2F7D08508EB255BF")
