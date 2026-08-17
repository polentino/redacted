addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix" % "0.14.7")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.3.1")
addSbtPlugin("io.kevinlee"    % "sbt-docusaur" % "0.22.0")

addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.22.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.12")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.4.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.4.0")

addDependencyTreePlugin
