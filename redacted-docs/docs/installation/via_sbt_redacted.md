---
sidebar_position: 1
---

# Via `sbt-redacted` (preferred)

Installing `@redacted` in your SBT project can be performed in few easy steps:


### 1. Add `sbt-redacted` plugin

This done by editing `project/plugins.sbt` file and adding the following line

```scala title="project/plugins.sbt"
addSbtPlugin("io.github.polentino" % "sbt-redacted" % "1.1.0")
```

### 2. Enable `RedactedPlugin`

Open `build.sbt` and enable `RedacedPlugin` to the root project or, in a multi-project / monorepo setup, to the specific
subproject you'd like to use `@redacted`, and set the version of the library you'd like to use. For example

```scala title="build.sbt"
lazy val root = (project in file("."))
  .enablePlugins(RedactedPlugin)
  .setting(
    redactedVersion := "0.7.1",
    // ... // your usual settings here
  )
```

That's it! All you have to now is ...


### 3. Use `@redacted` in your project !

```scala title="src/main/com/your/project/YourClass.scala"
package com.your.project

import io.github.polentino.redacted._

final case class YourClass(@redacted email: String, lastLogin: Timestamp)
```

