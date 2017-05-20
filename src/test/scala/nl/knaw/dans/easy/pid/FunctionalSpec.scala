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

import java.nio.file.Files

import org.scalatest.BeforeAndAfterEach

import scala.sys.process._
import scala.util.Success

class FunctionalSpec extends SeedDatabaseFixture with PropertiesSupportFixture with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()

    properties.properties.setProperty("pid-generator.database.url", s"jdbc:sqlite:${ databaseFile.toString }")
    properties.properties.save(testDir.resolve("cfg/application.properties").toFile)
    System.setProperty("app.home", testDir.toString)
  }

  "calling GET /" should "check that the service is up and running" in {
    PidGeneratorService.main(Array.empty)

    call("curl http://localhost:8060/pids") shouldBe "Persistent Identifier Generator running"
  }

  it should "return a 404 when using the incorrect url" in {
    PidGeneratorService.main(Array.empty)

    call("curl http://localhost:8060/") should {
      include ("Error 404 Not Found") and
        include ("HTTP ERROR 404") and
        include ("Problem accessing /. Reason:\n<pre>    Not Found")
    }
  }

  "calling POST /" should "return a 400" in {
    PidGeneratorService.main(Array.empty)

    call("curl -X POST http://localhost:8060/pids/urn") shouldBe "Cannot create PIDs at this URI"
  }

  "calling POST for URN" should "retrieve the first URN" in {
    PidGeneratorService.main(Array.empty)

    postUrn shouldBe "urn:nbn:nl:ui:13-0000-01"
    PidServiceWiring.database.getSeed(URN) shouldBe Success(Some(1L))
  }

  it should "retrieve the next URN if the service is called twice" in {
    PidGeneratorService.main(Array.empty)

    postUrn
    postUrn shouldBe "urn:nbn:nl:ui:13-001h-aq"
    PidServiceWiring.database.getSeed(URN) shouldBe Success(Some(69074L))
  }

  it should "fail if there are no more URN seeds" in {
    val lastSeed = 1752523756L
    PidServiceWiring.database.initSeed(URN, lastSeed) shouldBe a[Success[_]]

    PidGeneratorService.main(Array.empty)

    postUrn shouldBe "No more urn seeds available."
    PidServiceWiring.database.getSeed(URN) shouldBe Success(Some(lastSeed))
  }

  it should "fail if the service cannot connect to the database" in {
    Files.delete(databaseFile) // deleting the database so it cannot be connected to

    PidGeneratorService.main(Array.empty)

    postUrn shouldBe "Error when retrieving previous seed or saving current seed"
  }

  "calling POST for DOI" should "retrieve the first DOI" in {
    PidGeneratorService.main(Array.empty)

    postDoi shouldBe "10.5072/dans-x6f-kf6x"
    PidServiceWiring.database.getSeed(DOI) shouldBe Success(Some(1073741824L))
  }

  it should "retrieve the next DOI if the service is called twice" in {
    PidGeneratorService.main(Array.empty)

    postDoi
    postDoi shouldBe "10.5072/dans-x6f-kf66"
    PidServiceWiring.database.getSeed(DOI) shouldBe Success(Some(1073741829L))
  }

  it should "fail if there are no more DOI seeds" in {
    val lastSeed = 43171047L
    PidServiceWiring.database.initSeed(DOI, lastSeed) shouldBe a[Success[_]]

    PidGeneratorService.main(Array.empty)

    postDoi shouldBe "No more doi seeds available."
    PidServiceWiring.database.getSeed(DOI) shouldBe Success(Some(lastSeed))
  }

  it should "fail if the service cannot connect to the database" in {
    Files.delete(databaseFile) // deleting the database so it cannot be connected to

    PidGeneratorService.main(Array.empty)

    postDoi shouldBe "Error when retrieving previous seed or saving current seed"
  }

  private def call(command: String): String = (command !! ProcessLogger(_ => ())).trim
  private def postUrn = call("curl -X POST http://localhost:8060/pids?type=urn")
  private def postDoi = call("curl -X POST http://localhost:8060/pids?type=doi")
}
