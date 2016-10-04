/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid.microservice

import org.json4s.DefaultFormats
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class JsonTransformerSpec extends FlatSpec with Matchers {

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
