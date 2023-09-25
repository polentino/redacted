package io.github.polentino.redacted

import org.scalatest.Checkpoints._
import org.scalatest.flatspec.AnyFlatSpec

class PluginSpec extends AnyFlatSpec {

  final case class Testing(@redacted name: String, age: Int, @redacted email: String)

  private val name: String = "Berfu"
  private val age = 26
  private val email: String = "berfu@gmail.com"

  private val testing = Testing(name, age, email)
  private val expected = s"Testing(***,$age,***)"

  behavior of "@redacted"

  it should "hide sensitive data when dumping the whole object, but retain the individual values" in {
    val implicitToString = s"$testing"
    val explicitToString = testing.toString

    val cp = new Checkpoint
    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp { assert(testing.name == name && testing.age == age && testing.email == email) }
    cp.reportAll()
  }

  it should "have the same behaviour if the class is nested" in {
    final case class Wrapper(id: String, testing: Testing)
    val id = "id-1"
    val wrapper = Wrapper(id, testing)
    val expectedWrapper = s"Wrapper($id,Testing(***,$age,***))"

    val implicitToString = s"$wrapper"
    val explicitToString = wrapper.toString

    val cp = new Checkpoint
    cp { assert(implicitToString == expectedWrapper) }
    cp { assert(explicitToString == expectedWrapper) }
    cp {
      assert(
        wrapper.id == id &&
          wrapper.testing.name == name &&
          wrapper.testing.age == age &&
          wrapper.testing.email == email
      )
    }
    cp.reportAll()
  }

  it should "redact all fields, if the wrapper also annotates Testing with @redacted" in {
    final case class Wrapper(id: String, @redacted testing: Testing)
    val id = "id-1"
    val wrapper = Wrapper(id, testing)
    val expectedWrapper = s"Wrapper($id,***)"

    val implicitToString = s"$wrapper"
    val explicitToString = wrapper.toString

    val cp = new Checkpoint
    cp { assert(implicitToString == expectedWrapper) }
    cp { assert(explicitToString == expectedWrapper) }
    cp {
      assert(
        wrapper.id == id &&
          wrapper.testing.name == name &&
          wrapper.testing.age == age &&
          wrapper.testing.email == email
      )
    }
    cp.reportAll()
  }
}
