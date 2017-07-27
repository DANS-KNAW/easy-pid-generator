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

  override def beforeEach(): Unit = {
    super.beforeEach()
    app.init().unsafeGetOrThrow
  }

  override def afterEach(): Unit = {
    app.close()
    super.afterEach()
  }

  "generate(doi)" should "return the initial DOI when it is never called before and store this DOI in the database" in {
    app.generate(DOI) should matchPattern { case Success("10.5072/dans-x6f-kf6x") => }
    database.getSeed(DOI) should matchPattern { case Success(Some(1073741824L)) => }
  }

  it should "return the second DOI when it is called for the second PID and store this DOI in the database" in {
    app.generate(DOI)

    app.generate(DOI) should matchPattern { case Success("10.5072/dans-x6f-kf66") => }
    database.getSeed(DOI) should matchPattern { case Success(Some(1073741829L)) => }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    val endSeed = 43171047L
    database.initSeed(DOI, endSeed)

    app.generate(DOI) should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }
    database.getSeed(DOI) should matchPattern { case Success(Some(`endSeed`)) => }
  }

  "generate(urn)" should "return the initial URN when it is never called before and store this URN in the database" in {
    app.generate(URN) should matchPattern { case Success("urn:nbn:nl:ui:13-0000-01") => }
    database.getSeed(URN) should matchPattern { case Success(Some(1L)) => }
  }

  it should "return the second URN when it is called for the second PID and store this URN in the database" in {
    app.generate(URN)

    app.generate(URN) should matchPattern { case Success("urn:nbn:nl:ui:13-001h-aq") => }
    database.getSeed(URN) should matchPattern { case Success(Some(69074L)) => }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    val endSeed = 1752523756L
    database.initSeed(URN, endSeed)

    app.generate(URN) should matchPattern { case Failure(RanOutOfSeeds(URN)) => }
    database.getSeed(URN) should matchPattern { case Success(Some(`endSeed`)) => }
  }
}
