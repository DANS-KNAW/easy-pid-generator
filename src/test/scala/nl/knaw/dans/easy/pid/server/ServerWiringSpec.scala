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
package nl.knaw.dans.easy.pid.server

import nl.knaw.dans.easy.pid._
import nl.knaw.dans.easy.pid.generator._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatest.OneInstancePerTest

import scala.util.Success

class ServerWiringSpec extends ConfigurationSupportFixture with SeedDatabaseFixture with ServerTestSupportFixture with OneInstancePerTest
  with ServerWiring
  with DOIGeneratorWiring
  with URNGeneratorWiring
  with SeedStorageComponent
  with DatabaseComponent
  with PidFormatterComponent
  with DatabaseAccessComponent
  with DebugEnhancedLogging {

  val database: Database = new Database {}

  override def beforeEach(): Unit = {
    super.beforeEach()
    server.start() shouldBe a[Success[_]]
  }

  override def afterEach(): Unit = {
    server.stop() shouldBe a[Success[_]]
    server.destroy() shouldBe a[Success[_]]
    super.afterEach()
  }

  "serverPort" should "have the correct value based on the properties" in {
    server.serverPort shouldBe 8060
  }

  "get" should "indicate that the service is up and running" in {
    callService() shouldBe successful
  }

  "post urn" should "return a next URN" in {
    val urn = "urn:nbn:nl:ui:13-0000-01"
    postUrn shouldBe (200, urn)

    inside(database.getSeed(URN)) {
      case Success(Some(seed)) =>
        seed shouldBe 1L
        urnGenerator.formatter.format(seed) shouldBe urn
    }
  }

  "post doi" should "return a next DOI" in {
    val doi = "10.5072/dans-x6f-kf6x"
    postDoi shouldBe (200, doi)

    inside(database.getSeed(DOI)) {
      case Success(Some(seed)) =>
        seed shouldBe 1073741824L
        doiGenerator.formatter.format(seed) shouldBe doi
    }
  }
}
