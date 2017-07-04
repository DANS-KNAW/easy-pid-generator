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
package nl.knaw.dans.easy.pid.service

import java.nio.file.Files

import nl.knaw.dans.easy.pid._
import org.scalatest.{ BeforeAndAfterEach, OneInstancePerTest }

import scala.util.Success

class ServiceStarterSpec extends SeedDatabaseFixture with ConfigurationSupportFixture with TestSupportFixture with ServerTestSupportFixture with BeforeAndAfterEach with OneInstancePerTest {

  private lazy val daemon = new ServiceStarter
  private lazy val database = daemon.service.database

  override def beforeEach(): Unit = {
    super.beforeEach()

    configuration.properties.setProperty("pid-generator.database.url", s"jdbc:sqlite:${ databaseFile.toString }")
    configuration.properties.save(testDir.resolve("cfg/application.properties").toFile)
    System.setProperty("app.home", testDir.toString)

    daemon.init(null)
    daemon.start()
  }

  override def afterEach(): Unit = {
    daemon.stop()
    daemon.destroy()
    super.afterEach()
  }

  "calling GET /" should "check that the service is up and running" in {
    callService() shouldBe successful
  }

  it should "return a 404 when using the incorrect url" in {
    inside(callService("")) {
      case (404, body) =>
        body should {
          include("Error 404 Not Found") and
            include("HTTP ERROR 404") and
            include("Problem accessing /. Reason:\n<pre>    Not Found")
        }
    }
  }

  "calling POST /" should "return a 400" in {
    callService("pids/urn", "POST") shouldBe (400, "Cannot create PIDs at this URI")
  }

  "calling POST for URN" should "retrieve the first URN" in {
    postUrn shouldBe (200, "urn:nbn:nl:ui:13-0000-01")
    database.getSeed(URN) shouldBe Success(Some(1L))
  }

  it should "retrieve the next URN if the service is called twice" in {
    postUrn
    postUrn shouldBe (200, "urn:nbn:nl:ui:13-001h-aq")
    database.getSeed(URN) shouldBe Success(Some(69074L))
  }

  it should "fail if there are no more URN seeds" in {
    val lastSeed = 1752523756L
    database.initSeed(URN, lastSeed) shouldBe a[Success[_]]

    postUrn shouldBe (404, "No more urn seeds available.")
    database.getSeed(URN) shouldBe Success(Some(lastSeed))
  }

  it should "fail if the service cannot connect to the database" in {
    Files.delete(databaseFile) // deleting the database so it cannot be connected to

    postUrn shouldBe (500, "Error when retrieving previous seed or saving current seed")
  }

  "calling POST for DOI" should "retrieve the first DOI" in {
    postDoi shouldBe (200, "10.5072/dans-x6f-kf6x")
    database.getSeed(DOI) shouldBe Success(Some(1073741824L))
  }

  it should "retrieve the next DOI if the service is called twice" in {
    postDoi
    postDoi shouldBe (200, "10.5072/dans-x6f-kf66")
    database.getSeed(DOI) shouldBe Success(Some(1073741829L))
  }

  it should "fail if there are no more DOI seeds" in {
    val lastSeed = 43171047L
    database.initSeed(DOI, lastSeed) shouldBe a[Success[_]]

    postDoi shouldBe (404, "No more doi seeds available.")
    database.getSeed(DOI) shouldBe Success(Some(lastSeed))
  }

  it should "fail if the service cannot connect to the database" in {
    Files.delete(databaseFile) // deleting the database so it cannot be connected to

    postDoi shouldBe (500, "Error when retrieving previous seed or saving current seed")
  }
}
