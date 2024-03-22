package io.github.polentino.redacted

import org.scalatest.Checkpoints.*
import org.scalatest.flatspec.AnyFlatSpec

import io.github.polentino.redacted.RedactionWithNestedCaseClass.Inner

class RedactedSpec extends AnyFlatSpec {

  behavior of "@redacted"

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

  it should "work with a redacted case class with many members" in {
    case class ManyMembers(field1: String, @redacted field2: String, @redacted field3: String, field4: String)
    val field1 = "field-1"
    val field2 = "field-2"
    val field3 = "field-3"
    val field4 = "field-4"
    val expected = s"ManyMembers($field1,***,***,$field4)"

    val testing = ManyMembers(field1, field2, field3, field4)
    val implicitToString = s"$testing"
    val explicitToString = testing.toString

    val cp = new Checkpoint
    cp { assert(implicitToString == expected) }
    cp { assert(explicitToString == expected) }
    cp {
      assert(testing.field1 == field1 &&
        testing.field2 == field2 &&
        testing.field3 == field3 &&
        testing.field4 == field4)
    }
    cp.reportAll()
  }

  it should "work with nested case classes" in {
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
}
