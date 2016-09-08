package nl.knaw.dans.easy.pid.microservice

import org.json4s.DefaultFormats
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.util.{Failure, Success}

class JsonTransformerSpec extends FlatSpec with Matchers with OneInstancePerTest {

  case class TestObject(foo: String, bar: Int)
  case class NestedTestObject(identifier: String, nest: TestObject)

  val transformer = JsonTransformer(DefaultFormats)

  // We can't compare case classes after creating them using Json4s
  // so we compare the fields instead
  def isEqual(first: TestObject, second: TestObject): Boolean = {
    first.foo == second.foo && first.bar == second.bar
  }

  def isEqual(first: NestedTestObject, second: NestedTestObject): Boolean = {
    first.identifier == second.identifier && isEqual(first.nest, second.nest)
  }

  "parseJSON" should "transform a json string into the corresponding object" in {
    val o = transformer.parseJSON[TestObject]("""{"foo":"abc","bar":42}""")
    val expected = TestObject("abc", 42)

    o shouldBe a[Success[_]]
    isEqual(o.get, expected) shouldBe true
  }

  it should "transform a nested json string into the corresponding object" in {
    val o = transformer.parseJSON[NestedTestObject]("""{"identifier":"def","nest":{"foo":"abc", "bar":42}}""")
    val expected = NestedTestObject("def", TestObject("abc", 42))

    o shouldBe a[Success[_]]
    isEqual(o.get, expected) shouldBe true
  }

  it should "fail if the json does not reflect the class" in {
    val o = transformer.parseJSON[TestObject]("""{"first":"abc","second":42}""")

    o shouldBe a[Failure[_]]
  }

  "writeJSON" should "create the json from a given object" in {
    val o = transformer.writeJSON(TestObject("abc", 42))
    val expected = """{"foo":"abc","bar":42}"""

    o shouldBe expected
  }

  it should "create the json for a nested object structure" in {
    val o = transformer.writeJSON(NestedTestObject("def", TestObject("abc", 42)))
    val expected = """{"identifier":"def","nest":{"foo":"abc","bar":42}}"""

    o shouldBe expected
  }
}
