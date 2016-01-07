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
package nl.knaw.dans.easy.pid

import org.scalatest.Matchers
import org.scalatest.PropSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import Math.pow
import org.scalacheck.Gen

class PidPackageSpec extends PropSpec with GeneratorDrivenPropertyChecks with Matchers {

  property("Formatted PID starts with configured prefix") {
    forAll(Gen.posNum[Long],
      Gen.alphaStr,
      Gen.choose(2, 36),
      Gen.choose(5, 10)) {
        (pid: Long, prefix: String, radix: Int, len: Int) =>
          val result = format(prefix,
            radix,
            len,
            Map[Char, Char](),
            3)(pid)
          result should startWith(prefix)
      }
  }
  
  // TODO: Test more properties of format 
  
  property("insertDashAt inserts dash at given index") {
    forAll(Gen.alphaStr, Gen.posNum[Int]) { (s: String, i: Int) =>
      whenever(i < s.length) {
        val result = insertDashAt(i)(s)
        result(i) should be('-')
      }
    }
  }
}