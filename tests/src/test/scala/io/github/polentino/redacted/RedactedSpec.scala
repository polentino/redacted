package io.github.polentino.redacted

import org.scalatest.Checkpoints.*
import org.scalatest.flatspec.AnyFlatSpec

import io.github.polentino.redacted.RedactionWithNestedCaseClass.Inner

class RedactedSpec extends AnyFlatSpec {

  behavior of "@redacted"

  it should "work with case classes without user-defined companion object" in {
    val name: String = "Berfu"
    val age = 26
    val email: String = "berfu@gmail.com"
    val expected = s"RedactionWithoutCompanionObj(***,$age,***)"

    val testing = RedactionWithoutCompanionObj(name, age, email)
    val implicitToString = s"$testing"
    val explicitToString = testing.toString
    val cp = new Checkpoint

    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp { assert(testing.name == name && testing.age == age && testing.email == email) }
    cp.reportAll()
  }

  it should "work with case classes with user-defined companion object" in {
    val name: String = "Berfu"
    val age = 26
    val email: String = "berfu@gmail.com"
    val expected = s"RedactionWithCompanionObj(***,$age,***)"

    val testing = RedactionWithCompanionObj(name, age, email)
    val implicitToString = s"$testing"
    val explicitToString = testing.toString
    val cp = new Checkpoint

    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp { assert(testing.name == name && testing.age == age && testing.email == email) }
    cp { assert(RedactionWithCompanionObj.something == 123) }
    cp.reportAll()
  }

  it should "work with nested case classes in object" in {
    val id = "id-1"
    val name1 = "Diego"
    val age1 = 999
    val name2 = "Berfu"
    val age2 = 888
    val expected = s"RedactionWithNestedCaseClass($id,***,Inner(***,$age2))"

    val testing = RedactionWithNestedCaseClass(id, Inner(name1, age1), Inner(name2, age2))
    val implicitToString = s"$testing"
    val explicitToString = testing.toString

    val cp = new Checkpoint

    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp {
      assert(testing.id == id &&
        testing.inner1.name == name1 &&
        testing.inner1.age == age1 &&
        testing.inner2.name == name2 &&
        testing.inner2.age == age2)
    }
    cp.reportAll()
  }

  it should "work with nested case classes in case class" in {
    case class Inner(userId: String, @redacted balance: Int)
    case class Outer(inner: Inner)

    val userId = "user-123"
    val balance = 123_456_789
    val expected = s"Outer(Inner($userId,***))"

    val testing = Outer(Inner(userId, balance))
    val implicitToString = s"$testing"
    val explicitToString = testing.toString

    val cp = new Checkpoint

    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp {
      assert(
        testing.inner.userId == userId &&
          testing.inner.balance == balance
      )
    }
    cp.reportAll()
  }

  it should "work with a redacted case class of just one member" in {
    case class OneMember(@redacted name: String)
    val name = "berfu"
    val expected = "OneMember(***)"

    val testing = OneMember(name)
    val implicitToString = s"$testing"
    val explicitToString = testing.toString

    val cp = new Checkpoint

    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp { assert(testing.name == name) }
    cp.reportAll()
  }
}
