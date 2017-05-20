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

import java.sql.Connection

import nl.knaw.dans.easy.pid.{ RanOutOfSeeds, SeedDatabaseFixture, URN }
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

  override val database: Database = mock[Database]
  private val seedStore: SeedStorage = mock[SeedStorage]
  override val formatter: PidFormatter = mock[PidFormatter]

  val generator: PidGenerator = new PidGenerator {
    override val length: Int = 7
    override val illegalChars: Map[Char, Char] = Map('a' -> '0', 'b' -> '1')
    override val namespace: String = "test"
    override val dashPosition: Int = 3
    override val seedStorage: SeedStorage = seedStore
  }

  "next" should "calculate the next PID and format it according to the formatter" in {
    val formattedPid = "output"
    val nextPid = 96140546L

    (seedStore.calculateAndPersist(_: Long => Option[Long])(_: Connection)) expects
      (*, *) once() returning Success(nextPid)
    (formatter.format(_: String, _: Int, _: Int, _: Map[Char, Char], _: Int)(_: Long)) expects
      (generator.namespace,
        36 - generator.illegalChars.size,
        generator.length,
        generator.illegalChars,
        generator.dashPosition,
        nextPid) once() returning formattedPid

    generator.next() should matchPattern { case Success(`formattedPid`) => }
  }

  it should "fail if there is no new PID of the given type anymore" in {
    (seedStore.calculateAndPersist(_: Long => Option[Long])(_: Connection)) expects
      (*, *) once() returning Failure(RanOutOfSeeds(URN))
    (formatter.format(_: String, _: Int, _: Int, _: Map[Char, Char], _: Int)(_: Long)) expects
      (*, *, *, *, *, *) never()

    generator.next() should matchPattern { case Failure(RanOutOfSeeds(URN)) => }
  }
}
