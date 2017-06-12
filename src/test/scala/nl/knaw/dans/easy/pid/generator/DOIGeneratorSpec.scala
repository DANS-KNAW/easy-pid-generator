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
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success }

class DOIGeneratorSpec extends SeedDatabaseFixture
  with PropertiesSupportFixture
  with DOIGeneratorWiring
  with PropertiesComponent
  with SeedStorageComponent
  with DatabaseComponent
  with PidFormatterComponent
  with DebugEnhancedLogging {

  override val database: Database = new Database {}
  override val dois: DOIGenerator = new DOIGenerator {}

  "namespace" should "have the correct value based on the properties" in {
    dois.formatter.namespace shouldBe "10.5072/dans-"
  }

  "dashPosition" should "have the correct value based on the properties" in {
    dois.formatter.dashPosition shouldBe 3
  }

  "firstSeed" should "have the correct value based on the properties" in {
    dois.seedStorage.firstSeed shouldBe 1073741824L
  }

  "next" should "return the initial DOI when it is never called before and store this DOI in the database" in {
    val doi = dois.next()
    doi shouldBe Success("10.5072/dans-x6f-kf6x")

    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) =>
        seed shouldBe 1073741824L
        dois.formatter.format(seed) shouldBe doi.get
    }
  }

  it should "return the second DOI when it is called for the second PID and store this DOI in the database" in {
    dois.next() shouldBe a[Success[_]]
    val doi = dois.next()
    doi shouldBe Success("10.5072/dans-x6f-kf66")

    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) =>
        seed shouldBe 1073741829L
        dois.formatter.format(seed) shouldBe doi.get
    }
  }

  it should "fail when the seed in the database is the last seed available and leave the database unchanged" in {
    database.initSeed(DOI, 43171047L) shouldBe a[Success[_]]
    dois.next() should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }

    database.getSeed(DOI) shouldBe Success(Some(43171047L))
  }
}
