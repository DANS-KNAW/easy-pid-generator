/*
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid.generator

import nl.knaw.dans.easy.pid.Seed
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.language.postfixOps

class PidFormatterSpec extends AnyPropSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  def genMap(n: Int): Gen[Map[Char, Char]] = {
    Gen.mapOf(for {
      a <- Gen.alphaChar
      b <- Gen.alphaChar
    } yield a -> b).map(_.take(n))
  }

  property("Formatted PID starts with configured prefix") {
    forAll(Gen.posNum[Seed], Gen.alphaStr, Gen.choose(2, 36), Gen.choose(5, 10)) {
      (pid: Seed, prefix: String, radix: Int, len: Int) =>
        PidFormatter.format(prefix, radix, len, Map.empty[Char, Char], 3)(pid) should startWith(prefix)
    }
  }

  property("If the seed is converted correctly, it should start with a number of 0's") {
    forAll(Gen.posNum[Seed], Gen.choose(2, 36), Gen.choose(5, 10), genMap(4)) {
      (seed: Seed, radix: Int, len: Int, charMap: Map[Char, Char]) => {
        val radixedSeedLength = java.lang.Long.toString(seed, radix).length
        whenever(radixedSeedLength < len && !charMap.contains('0')) {
          PidFormatter.convertToString(seed, radix, len, charMap) should startWith("0" * (radixedSeedLength - len))
        }
      }
    }
  }

  property("insertDashAt inserts dash at given index") {
    forAll(Gen.alphaStr, Gen.posNum[Int]) { (s: String, i: Int) =>
      whenever(i < s.length) {
        val result = PidFormatter.insertDashAt(i)(s)
        result(i) shouldBe '-'
      }
    }
  }
}
