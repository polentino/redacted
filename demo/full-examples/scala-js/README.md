# Scala.js Node.js Webserver

A Scala.js application targeting Node.js that spins up a lightweight webserver. The server listens for `POST` requests at `/helloworld` and expects a JSON body with `name`, `age`, and `email` fields.

---

## Project Structure

* **[build.sbt](file:///Users/dc/projects/tests/build.sbt)**: Scala.js and SBT build configuration targeting Node.js CommonJS module output.
* **[project/plugins.sbt](file:///Users/dc/projects/tests/project/plugins.sbt)**: Declares the Scala.js and sbt-redacted plugins.
* **[src/main/scala/Main.scala](file:///Users/dc/projects/tests/src/main/scala/Main.scala)**: Webserver codebase utilizing Node.js native `http` module facades and a redacted user case class.

---

## Prerequisites

Make sure you have the following installed:
* **Java SDK** (v17 or higher)
* **sbt** (Scala Build Tool)
* **Node.js**

---

## 1. Build the Project

Compile the Scala files and optimize/link them into JavaScript:

```bash
sbt compile fastLinkJS
```

This will produce the compiled JavaScript files in `target/scala-3.7.4/scalajs-hello-world-webserver-fastopt/`.

---

## 2. Run the Webserver

Start the webserver using Node.js:

```bash
node target/scala-3.7.4/scalajs-hello-world-webserver-fastopt/main.js
```

You should see the console output:
```text
Server running on port 8080
```

---

## 3. Test the Endpoint (curl)

The server only accepts a JSON body for `POST` requests to `/helloworld`. Execute the following curl command:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"name": "Berfu", "age": 29, "email": "berfu@example.com"}' \
  "http://localhost:8080/helloworld"
```

### Expected Output
```text
Found user User(Alice,***,***) with fields:
	* age   = 30
	* email = alice@example.com
```

*(Note that the `age` and `email` fields are redacted in the `User` class `toString` representation due to the `@redacted` annotation, but are successfully processed by the server).*
