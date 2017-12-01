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

import nl.knaw.dans.easy.pid._
import nl.knaw.dans.easy.pid.fixture.TestSupportFixture
import nl.knaw.dans.easy.pid.seedstorage.{ Database, SeedStorageComponent }
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class PidGeneratorSpec extends TestSupportFixture with MockFactory with PidGeneratorComponent with SeedStorageComponent {
  val database: Database = mock[Database]
  val formatter: PidFormatter = mock[PidFormatter]
  override val seedStorage: SeedStorage = mock[SeedStorage]
  override val pidGenerator: PidGenerator = new PidGenerator(Map(DOI -> formatter))

  "generate" should "calculate the next PID and format it according to the formatter" in {
    val formattedPid = "output"
    val nextPid = 96140546L

    (seedStorage.calculateAndPersist(_: PidType)(_: Seed => Seed)) expects (DOI, *) once() returning Success(nextPid)
    formatter.format _ expects nextPid once() returning formattedPid

    pidGenerator.generate(DOI) should matchPattern { case Success(`formattedPid`) => }
  }

  it should "fail if there is no new PID of the given type anymore" in {
    (seedStorage.calculateAndPersist(_: PidType)(_: Seed => Seed)) expects (DOI, *) once() returning Failure(RanOutOfSeeds(DOI))
    formatter.format _ expects * never()

    pidGenerator.generate(DOI) should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }
  }
}
