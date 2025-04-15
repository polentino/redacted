# Development

## Project Structure

`@redacted` repository is composed of multiple SBT subprojects:

1. `./library` which contains the actual `@redacted` annotation definition
2. `./plugin` which contains the scala compiler plugin logic for redacted (we will see this in detail below)
3. `./redacted-docs` which contains the source of redacted GitHub website (using [docusaurus](http://docusaurus.io/))
4. `./test` which contains a battery of tests that will be

We won't be seeing the structure of `./library` since it contains a one-liner annotation definition, nor
`./redacted-docs`, since it's outside the scope of this guide and there are plenty of excellent resources on building
sites with docusaurus and integrate them with SBT and GitHub.

## Redacted Scala Compiler Plugin

As we just discussed, `./plugin` hosts the bulk of the Scala compiler plugin logic that is responsible for

* register itself as a valid Scala (both `2.x` and `3.x`) compiler plugin
* analyse the typed AST that the scala compiler is currently compiling
* override, if the necessary conditions are met, the original `toString` in order to hide the `@redacted` fields

As you might know, Scala `2.x` and `3.x` have different compiler plugin APIs, therefore "writing one single codebase
to rule them all" doesn't apply here.

However, there are some similarities and commonalities that can be extracted and generalised: we will see them in a
short while, case-by-case.

### `resources` folder

The [resources](https://github.com/polentino/redacted/tree/main/plugin/src/main/resources) folder contains the
configuration files needed to register redacted compiler plugin itself into the Scala compiler:

* `scalac-plugin.xml` is the configuration file needed for Scala `2.x`
* `plugin.properties` is the configuration file needed for Scala `3.x`

They are bundled all together in the same artifact (after all, depending on the Scala runtime, only one will be
recognised).

### `scala` folder

The [src/main/scala](https://github.com/polentino/redacted/tree/main/plugin/src/main/scala) folder contains the Scala
version-agnostic sources of the compiler plugin: they expose the generic interfaces and data types needed for the
compiler plugin to work, and abstract the logic to determine if the `Tree` being currently analysed by the Scala
compiler needs to be patched.

#### `RuntimeApi`

`RuntimeApi` is the main trait that needs to be implemented for each Scala version.

It contains the types definitions that it requires in order to work properly, such as `Tree`, `DefDef`, `Symbol` and so
on, as well the abstract method definitions that needs to be implemented to work properly, for example `caseClassOwner`
which, given a `Tree` as parameter, returns an `Option[Symbol]` describing the case class that is associated to it, if
any.

It also contains methods, such as `process`, `validate` and `createToStringBody`, which are using those abstract methods
to build the whole core of the plugin:

* `validate` makes sure that:
    * the `Tree` under analysis correspond to a `toString` method
    * tries to return its case class owner, if any
    * tries to retrieve all the owner's redacted fields
    * bundles the results together if the all the checks pass
* `createToStringBody` uses the results of the validation step to build the AST equivalent to the redacted `toString`
  method
* `process` puts the steps above together, and prints an error report in case something went wrong

#### `ReporterApi`

There's also a small utility, `ReporterApi`, to generalise the reporter functionality that is present in the Scala `2.x`
and `3.x` versions.

#### `RedactedApi`

`RedactedApi`, is another 

### `scala-2` and `scala-3` folders

The [src/main/scala-2](https://github.com/polentino/redacted/tree/main/plugin/src/main/scala-2) and
[src/main/scala-3](https://github.com/polentino/redacted/tree/main/plugin/src/main/scala-3) folder contains the sources
which are Scala version-agnostic: they expose the generic interfaces and data types