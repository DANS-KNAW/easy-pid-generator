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
package nl.knaw.dans.easy.pid

import nl.knaw.dans.easy.pid.fixture.{ ConfigurationSupportFixture, SeedDatabaseFixture, TestSupportFixture }
import nl.knaw.dans.easy.pid.seedstorage.Database
import org.scalatest.OneInstancePerTest

import scala.util.{ Failure, Success }

class PidGeneratorAppSpec extends TestSupportFixture
  with OneInstancePerTest
  with SeedDatabaseFixture
  with ConfigurationSupportFixture {

  val database = new Database
  val app = new PidGeneratorApp(new ApplicationWiring(configuration))

  "generate(doi)" should "return the initial DOI when it is never called before and store this DOI in the database" in {
    val result = app.generate(DOI)

    inside(result) {
      case Success(doi) => doi shouldBe "10.5072/dans-x6f-kf6x"
    }
  }

  // TODO: STYLE: this more verbose style makes it clearer what the test scenario is.
  it should "return the second DOI when it is called for the second PID and store this DOI in the database" in {
    val result1 = app.generate(DOI)
    val result2 = app.generate(DOI)

    result1 shouldBe a[Success[_]]
    result2 shouldBe a[Success[_]]
    inside(result2) {
      case Success(doi) => doi shouldBe "10.5072/dans-x6f-kf66" // TODO: Is this the second??? To similar to the first!!
    }
    inside(database.getSeed(DOI)) {
      case Success(Some(doi)) => doi shouldBe 1073741829 // TODO: check that this actually the correct result!
    }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    val endSeed = 43171047L
    database.initSeed(DOI, endSeed) // TODO: STYLE: this is a preparatory action: no "SHOULD" test!
    val result = app.generate(DOI)

    result should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }
    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) => seed shouldBe endSeed
    }
  }

  "generate(urn)" should "return the initial URN when it is never called before and store this URN in the database" in {
    val result = app.generate(URN)

    inside(result) {
      case Success(urn) => urn shouldBe "urn:nbn:nl:ui:13-0000-01"
    }
    inside(database.getSeed(URN)) {
      case Success(Some(seed)) => seed shouldBe 1L
    }
  }

  it should "return the second URN when it is called for the second PID and store this URN in the database" in {
    val result1 = app.generate(URN)
    val result2 = app.generate(URN)

    inside(result2) {
      case Success(urn) => urn shouldBe "urn:nbn:nl:ui:13-001h-aq"
    }
    inside(database.getSeed(URN)) {
      case Success(Some(seed)) => seed shouldBe 69074L
    }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    database.initSeed(URN, 1752523756L)
    val result = app.generate(URN)

    result should matchPattern { case Failure(RanOutOfSeeds(URN)) => }
    inside(database.getSeed(URN)) {
      case Success(Some(seed)) => seed shouldBe 1752523756L
    }
  }
}
