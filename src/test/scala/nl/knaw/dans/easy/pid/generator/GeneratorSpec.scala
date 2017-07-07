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

import scala.util.{ Failure, Success }

class GeneratorSpec extends TestSupportFixture
  with SeedDatabaseFixture
  with ConfigurationSupportFixture
  with GeneratorWiring
  with SeedStorageComponent
  with DatabaseComponent
  with PidFormatterComponent {

  override val database: Database = new Database {}

  "doi namespace" should "have the correct value based on the properties" in {
    doiGenerator.formatter.namespace shouldBe "10.5072/dans-"
  }

  "doi dashPosition" should "have the correct value based on the properties" in {
    doiGenerator.formatter.dashPosition shouldBe 3
  }

  "doi firstSeed" should "have the correct value based on the properties" in {
    doiGenerator.seedStorage.firstSeed shouldBe 1073741824L
  }

  "doi next" should "return the initial DOI when it is never called before and store this DOI in the database" in {
    val doi = doiGenerator.next()
    doi shouldBe Success("10.5072/dans-x6f-kf6x")

    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) =>
        seed shouldBe 1073741824L
        doiGenerator.formatter.format(seed) shouldBe doi.get
    }
  }

  it should "return the second DOI when it is called for the second PID and store this DOI in the database" in {
    doiGenerator.next() shouldBe a[Success[_]]
    val doi = doiGenerator.next()
    doi shouldBe Success("10.5072/dans-x6f-kf66")

    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) =>
        seed shouldBe 1073741829L
        doiGenerator.formatter.format(seed) shouldBe doi.get
    }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    database.initSeed(DOI, 43171047L) shouldBe a[Success[_]]
    doiGenerator.next() should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }

    database.getSeed(DOI) shouldBe Success(Some(43171047L))
  }

  "urn namespace" should "have the correct value based on the properties" in {
    urnGenerator.formatter.namespace shouldBe "urn:nbn:nl:ui:13-"
  }

  "urn dashPosition" should "have the correct value based on the properties" in {
    urnGenerator.formatter.dashPosition shouldBe 4
  }

  "urn firstSeed" should "have the correct value based on the properties" in {
    urnGenerator.seedStorage.firstSeed shouldBe 1L
  }

  "urn next" should "return the initial URN when it is never called before and store this URN in the database" in {
    val urn = urnGenerator.next()
    urn shouldBe Success("urn:nbn:nl:ui:13-0000-01")

    inside(database.getSeed(URN)) {
      case Success(Some(seed)) =>
        seed shouldBe 1L
        urnGenerator.formatter.format(seed) shouldBe urn.get
    }
  }

  it should "return the second URN when it is called for the second PID and store this URN in the database" in {
    urnGenerator.next() shouldBe a[Success[_]]
    val urn = urnGenerator.next()
    urn shouldBe Success("urn:nbn:nl:ui:13-001h-aq")

    inside(database.getSeed(URN)) {
      case Success(Some(seed)) =>
        seed shouldBe 69074L
        urnGenerator.formatter.format(seed) shouldBe urn.get
    }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    database.initSeed(URN, 1752523756L) shouldBe a[Success[_]]
    urnGenerator.next() should matchPattern { case Failure(RanOutOfSeeds(URN)) => }

    database.getSeed(URN) shouldBe Success(Some(1752523756L))
  }
}
