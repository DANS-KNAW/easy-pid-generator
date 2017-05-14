/**
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

import nl.knaw.dans.easy.pid.{ PidType, RanOutOfSeeds, SeedDatabaseFixture, URN }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class PidGeneratorSpec extends SeedDatabaseFixture
  with MockFactory
  with PidGeneratorComponent
  with SeedStorageComponent
  with PidFormatterComponent
  with DatabaseComponent
  with DebugEnhancedLogging {

  val database: Database = new Database {}
  val seedStorage: SeedStorage = new SeedStorage {
    val pidType: PidType = URN
    val firstSeed: Long = 1L
  }
  val formatter: PidFormatter = mock[PidFormatter]
  val generator: PidGenerator = new PidGenerator {
    val length: Int = 7
    val illegalChars: Map[Char, Char] = Map('a' -> '0', 'b' -> '1')
    val namespace: String = "test"
    val dashPosition: Int = 3
  }

  "next" should "calculate the next PID and format it according to the formatter" in {
    val formattedPid = "output"
    val pid = 654321L
    val next = 96140546L

    database.initSeed(seedStorage.pidType, pid) shouldBe a[Success[_]]
    (formatter.format(_: String, _: Int, _: Int, _: Map[Char, Char], _: Int)(_: Long)) expects
      ("test", 34, 7, Map('a' -> '0', 'b' -> '1'), 3, next) once() returning formattedPid

    generator.next() should matchPattern { case Success(`formattedPid`) => }
    database.getSeed(seedStorage.pidType) should matchPattern { case Success(Some(`next`)) => }
  }

  it should "not return a next PID if the newly calculated PID is equal to the initial seed" in {
    val pid = 1752523756L // with this PID, the next PID is 1L, which is equal to the initial PID

    database.initSeed(seedStorage.pidType, pid) shouldBe a[Success[_]]
    (formatter.format(_: String, _: Int, _: Int, _: Map[Char, Char], _: Int)(_: Long)) expects
      (*, *, *, *, *, *) never()

    generator.next() should matchPattern { case Failure(RanOutOfSeeds(URN)) => }
  }
}
