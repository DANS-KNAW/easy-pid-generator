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

import nl.knaw.dans.easy.pid.{ RanOutOfSeeds, SeedDatabaseFixture, TestSupportFixture, URN }
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class PidGeneratorSpec extends TestSupportFixture
  with SeedDatabaseFixture
  with MockFactory
  with PidGeneratorComponent
  with SeedStorageComponent
  with PidFormatterComponent
  with DatabaseComponent {

  override val database: Database = mock[Database]
  private val seedStore: SeedStorage = mock[SeedStorage]
  private val pidFormatter = mock[PidFormatter]

  val generator: PidGenerator = new PidGenerator {
    override val seedStorage: SeedStorage = seedStore
    override val formatter: PidFormatter = pidFormatter
  }

  "next" should "calculate the next PID and format it according to the formatter" in {
    val formattedPid = "output"
    val nextPid = 96140546L

    (seedStore.calculateAndPersist(_: Long => Long)(_: Connection)) expects
      (*, *) once() returning Success(nextPid)
    pidFormatter.format _ expects nextPid once() returning formattedPid

    generator.next() should matchPattern { case Success(`formattedPid`) => }
  }

  it should "fail if there is no new PID of the given type anymore" in {
    (seedStore.calculateAndPersist(_: Long => Long)(_: Connection)) expects
      (*, *) once() returning Failure(RanOutOfSeeds(URN))
    pidFormatter.format _ expects * never()

    generator.next() should matchPattern { case Failure(RanOutOfSeeds(URN)) => }
  }
}
