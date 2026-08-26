import io.github.polentino.redacted.redacted

object Main {
  final case class User(name: String, @redacted age: Int, @redacted email: String)

  def main(args: Array[String]): Unit = {
    val user = User("Berfu", 29, "some.email@corp.org")
    println(
      s"""
         |Found user $user with fields:
         |\t* age   = ${user.age}
         |\t* email = ${user.email}
         |""".stripMargin)
  }
}
