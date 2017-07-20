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
package nl.knaw.dans.easy.pid2.server

import nl.knaw.dans.easy.pid2._
import nl.knaw.dans.easy.pid2.generator._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }

class PidServletSpec extends TestSupportFixture
  with ConfigurationSupportFixture
  with SeedDatabaseFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  class MockedPidGeneratorApp extends PidGeneratorApp(null)

  val app: PidGeneratorApp = mock[MockedPidGeneratorApp]
  val pidServlet: PidServlet = new PidServlet(app)

  addServlet(pidServlet, "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      status shouldBe 200
      body shouldBe "Persistent Identifier Generator running"
    }
  }

  "post /*" should "return a failure response, because the url is incorrect" in {
    post("/foo") {
      status shouldBe 400
      body shouldBe "Cannot create PIDs at this URI"
    }
  }

  "post with URN request" should "return the next URN PID" in {
    (app.generate(_: PidType)) expects URN once() returning Success("urn output")
    post("/", ("type", "urn")) {
      status shouldBe 200
      body shouldBe "urn output"
    }
  }

  it should "return a failure if the generator ran out of seeds" in {
    (app.generate(_: PidType)) expects URN once() returning Failure(RanOutOfSeeds(URN))
    post("/", ("type", "urn")) {
      status shouldBe 404
      body shouldBe "No more urn seeds available."
    }
  }

  it should "return a failure if the generator failed unexpectedly" in {
    (app.generate(_: PidType)) expects URN once() returning Failure(new Exception("unexpected failure"))
    post("/", ("type", "urn")) {
      status shouldBe 500
      body shouldBe "Error when retrieving previous seed or saving current seed"
    }
  }

  "post with DOI request" should "return the next DOI PID" in {
    (app.generate(_: PidType)) expects DOI once() returning Success("doi output")
    post("/", ("type", "doi")) {
      status shouldBe 200
      body shouldBe "doi output"
    }
  }

  it should "return a failure if the generator ran out of seeds" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(RanOutOfSeeds(DOI))
    post("/", ("type", "doi")) {
      status shouldBe 404
      body shouldBe "No more doi seeds available."
    }
  }

  it should "return a failure if the generator failed unexpectedly" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(new Exception("unexpected failure"))
    post("/", ("type", "doi")) {
      status shouldBe 500
      body shouldBe "Error when retrieving previous seed or saving current seed"
    }
  }

  "post with an unknown type" should "return a 400 error" in {
    post("/", ("type", "unknown")) {
      status shouldBe 400
      body shouldBe "Unknown PID type 'unknown'"
    }
  }

  "post with no request type" should "default to requesting a DOI" in {
    (app.generate(_: PidType)) expects DOI once() returning Success("doi output")
    post("/") {
      status shouldBe 200
      body shouldBe "doi output"
    }
  }
}
