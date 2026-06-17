# @redacted example for Scala Native

This folder contains a simple sbt project to showcase `@redacted` in conjunction with Scala Native.

## Instructions

Build the native executable via

```shell
sbt run
```

this will create your target executable under `./target/scala-3.7.4/` named `redacted-native`.

Then, simply execute it like so

```shell
./target/scala-3.7.4/redacted-native
```

which will print out

```terminaloutput
Found user User(Berfu,***,***) with fields:
        * age   = 29
        * email = some.email@corp.org
```