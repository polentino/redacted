package io.github.polentino.redacted

import org.scalatest.Checkpoints.*
import org.scalatest.flatspec.AnyFlatSpec
import io.github.polentino.redacted.RedactionWithNestedCaseClass.Inner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID

class RedactedSpec extends AnyFlatSpec with ScalaCheckPropertyChecks {

  behavior of "@redacted"

  it should "work with a redacted case class of just one member" in {
    case class OneMember(@redacted name: String)

    forAll { (name: String) =>
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

  it should "work with a redacted case class with many members" in {
    case class ManyMembers(field1: String, @redacted field2: String, @redacted field3: String, field4: String)

    forAll { (field1: String, field2: String, field3: String, field4: String) =>
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
  }

  it should "work with case class nested in companion object" in {
    forAll { (id: String, name1: String, age1: Int, name2: String, age2: Int) =>
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
  }

  it should "work with nested case classes in case class" in {
    case class Inner(userId: String, @redacted balance: Int)
    case class Outer(inner: Inner)

    forAll { (userId: String, balance: Int) =>
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

  it must "not change the behavior of `hashCode`" in {
    final case class TestClass(uuid: UUID, name: String, age: Int)
    object RedactedTestClass {
      final case class TestClass(uuid: UUID, @redacted name: String, @redacted age: Int)
    }

    forAll { (uuid: UUID, name: String, age: Int) =>
      val testClass = TestClass(uuid, name, age)
      val redactedTestClass = RedactedTestClass.TestClass(uuid, name, age)

      assert(testClass.hashCode() == redactedTestClass.hashCode())
    }
  }
}
