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

import nl.knaw.dans.easy.pid.fixture.{ ConfigurationSupportFixture, ServletFixture, TestSupportFixture }
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }

class PidGeneratorServletSpec extends TestSupportFixture
  with ConfigurationSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  class MockedPidGeneratorApp extends PidGeneratorApp(null: ApplicationWiring)

  val app: PidGeneratorApp = mock[MockedPidGeneratorApp]
  val pidServlet: PidGeneratorServlet = new PidGeneratorServlet(app)

  addServlet(pidServlet, "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      status shouldBe 200
      body shouldBe "Persistent Identifier Generator running"
    }
  }

  "post with DOI request" should "return the next DOI PID" in {
    (app.generate(_: PidType)) expects DOI once() returning Success("doi output")
    post("/", ("type", "doi")) {
      status shouldBe 200
      body shouldBe "doi output"
    }
  }

  it should "return a 500 when the database connection fails suddenly" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(DatabaseException(new Exception("test")))
    post("/", ("type", "doi")) {
      status shouldBe 500
      body should include("database connection failed")
    }
  }

  it should "return a 500 when the generator is not initialized" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(SeedNotInitialized(DOI))
    post("/", ("type", "doi")) {
      status shouldBe 500
      body should include("not yet initialized")
    }
  }

  it should "return a 500 when the generator encounters a duplicate Pid" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(DuplicatePid(DOI, 1L, 2L, "testpid", new DateTime(1992, 7, 30, 16, 1)))
    post("/", ("type", "doi")) {
      status shouldBe 500
      body should include("Duplicate doi detected: testpid.")
    }
  }

  it should "return a 500 when the generator failed unexpectedly" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(new Exception("unexpected failure"))
    post("/", ("type", "doi")) {
      status shouldBe 500
      body shouldBe "Error when generating the next doi: unexpected failure"
    }
  }

  "post with an unknown type" should "return a 400 error" in {
    post("/", ("type", "unknown")) {
      status shouldBe 400
      body shouldBe "Unknown PID type 'unknown'"
    }
  }

  "post with no request type" should "default to requesting a DOI" in {
    post("/") {
      status shouldBe 400
      body should include("No Pid type specified")
    }
  }
}
