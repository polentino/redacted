import io.github.polentino.redacted.redacted

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("http", JSImport.Namespace)
object Http extends js.Object {
  def createServer(callback: js.Function2[IncomingMessage, ServerResponse, Unit]): Server = js.native
}

@js.native
trait Server extends js.Object {
  def listen(port: Int, callback: js.Function0[Unit]): Server = js.native
}

@js.native
trait IncomingMessage extends js.Object {
  def method: String = js.native

  def url: String = js.native

  def on(event: String, listener: js.Function): IncomingMessage = js.native
}

@js.native
trait ServerResponse extends js.Object {
  def writeHead(statusCode: Int, headers: js.Dictionary[String]): ServerResponse = js.native

  def write(chunk: String): ServerResponse = js.native

  def end(): Unit = js.native

  def end(chunk: String): Unit = js.native
}

@js.native
@JSGlobal("URL")
class URL(url: String, base: String) extends js.Object {
  def pathname: String = js.native
}

object Main {
  final case class User(name: String, @redacted age: Int, @redacted email: String)

  def main(args: Array[String]): Unit = {
    val port = 8080
    val server = Http.createServer((req: IncomingMessage, res: ServerResponse) => {
      val path = new URL(req.url, "http://localhost").pathname

      if (path == "/helloworld" && req.method == "POST") {
        var body = ""
        req.on(
          "data",
          (chunk: js.Any) => {
            body += chunk.toString()
          })

        req.on(
          "end",
          () => {
            val parsed = js.JSON.parse(body).asInstanceOf[js.Dynamic]

            val userResponse = for {
              name <- if (!js.isUndefined(parsed.name)) Option(parsed.name).map(_.toString) else None
              age <-
                if (!js.isUndefined(parsed.age)) Option(parsed.age).map(_.toString).flatMap(_.toIntOption) else None
              email <- if (!js.isUndefined(parsed.email)) Option(parsed.email).map(_.toString) else None
              user = User(name, age, email)
            } yield s"""
               |Saved user $user with fields:
               |\t* age   = ${user.age}
               |\t* email = ${user.email}
               |""".stripMargin

            val (code, response) = userResponse match {
              case Some(message) => (200, message)
              case _             => (400, "something went wrong")
            }

            println(response)

            res.writeHead(code, js.Dictionary("Content-Type" -> "text/plain"))
            res.end(response)
          }
        )
      } else {
        res.writeHead(400, js.Dictionary("Content-Type" -> "text/html"))
        res.end(
          """
            |<h3>Something went wrong</h3>
            |Usage example:
            |<br/>
            |<br/>
            |<quote>
            |curl -X POST \<br/>
            |&nbsp;&nbsp;-H "Content-Type: application/json" \<br/>
            |&nbsp;&nbsp;-d '{"name": "Berfu", "age": 29, "email": "berfu@example.com"}' \<br/>
            |&nbsp;&nbsp;"http://localhost:8080/helloworld"
            |</quote>""".stripMargin)
      }
    })

    server.listen(
      port,
      () => {
        println(s"Server running on port $port")
      })
  }
}
