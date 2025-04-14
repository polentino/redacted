---
sidebar_position: 2
---

# Via library dependencies

Despite installation [via sbt-redacted](via_sbt_redacted) is the recommended way to add `@redacted` to your existing
project, you can also opt for adding manually the required dependencies.

The procedure is pretty straightforward although, can't stress it enough, using `sbt-plugin` would be more future-proof,
since there are plans to extend the compiler plugin, and using the sbt plugin would make its configuration less
error-prone.

If you're still convinced, all you have to do is edit `build.sbt` like so

```scala title="build.sbt"
lazy val redactedVersion = "x.y.z" // use latest version of the library
// resolvers += DefaultMavenRepository

libraryDependencies ++= Seq(
  "io.github.polentino" %% "redacted" % redactedVersion cross CrossVersion.full,
  compilerPlugin("io.github.polentino" %% "redacted-plugin" % redactedVersion cross CrossVersion.full)
)
```

Once done that, just import the annotation in your `.scala` file, and use it like so

```scala title="src/main/com/your/project/YourClass.scala"
package com.your.project

import io.github.polentino.redacted._

final case class YourClass(@redacted email: String, lastLogin: Timestamp)
```