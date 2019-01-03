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

import java.sql.SQLException

import nl.knaw.dans.easy.pid.fixture.{ ConfigurationSupportFixture, TestSupportFixture }
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }

class PidGeneratorServletSpec extends TestSupportFixture
  with ConfigurationSupportFixture
  with EmbeddedJettyContainer
  with ScalatraSuite
  with MockFactory {

  class MockedPidGeneratorApp extends PidGeneratorApp(null: ApplicationWiring)

  val app: PidGeneratorApp = mock[MockedPidGeneratorApp]
  val pidServlet: PidGeneratorServlet = new PidGeneratorServlet(app, configuration)

  addServlet(pidServlet, "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      status shouldBe 200
      body shouldBe "Persistent Identifier Generator running (v1.0.0-UNITTEST)"
    }
  }

  "GET /doi/<doi>" should "return 204 when the DOI is present" in {
    app.exists _ expects (DOI, "my-doi") once() returning Success(true)
    get("/doi/my-doi") {
      status shouldBe 204
    }
  }

  it should "return 404 when the DOI is not present" in {
    app.exists _ expects (DOI, "my-doi") once() returning Success(false)
    get("/doi/my-doi") {
      status shouldBe 404
      body shouldBe "doi my-doi doesn't exist"
    }
  }

  it should "return 400 when an unknown PID type is requested" in {
    get("/unknown/my-pid") {
      status shouldBe 400
      body shouldBe "Usage: GET /{doi|urn}/{...}"
    }
  }

  "POST /create?type=doi" should "return the next DOI PID" in {
    (app.generate(_: PidType)) expects DOI once() returning Success("doi output")
    post("/create", ("type", "doi")) {
      status shouldBe 201
      body shouldBe "doi output"
    }
  }

  it should "return a 500 when the database connection fails suddenly" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(DatabaseException(new Exception("test")))
    post("/create", ("type", "doi")) {
      status shouldBe 500
      body should include("database connection failed")
    }
  }

  it should "return a 500 when the generator is not initialized" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(PidNotInitialized(DOI))
    post("/create", ("type", "doi")) {
      status shouldBe 500
      body should include("not yet initialized")
    }
  }

  it should "return a 500 when the generator encounters a duplicate Pid" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(DuplicatePid(DOI, 1L, 2L, "testpid", new DateTime(1992, 7, 30, 16, 1)))
    post("/create", ("type", "doi")) {
      status shouldBe 500
      body should include("Duplicate doi detected: testpid.")
    }
  }

  it should "return a 500 when the generator failed unexpectedly" in {
    (app.generate(_: PidType)) expects DOI once() returning Failure(new Exception("unexpected failure"))
    post("/create", ("type", "doi")) {
      status shouldBe 500
      body shouldBe "Error while generating the next doi: unexpected failure"
    }
  }

  "POST /create?type=unknown" should "return a 400 error" in {
    post("/create", ("type", "unknown")) {
      status shouldBe 400
      body should startWith("No or unknown Pid type specified")
    }
  }

  "POST /create" should "fail because no type was specified" in {
    post("/create") {
      status shouldBe 400
      body should startWith("No or unknown Pid type specified")
    }
  }

  "POST /init?type=doi&seed=123456" should "return a 201 when the DOI is set correctly" in {
    val seed = 123456L
    app.initialize _ expects(DOI, seed) once() returning Success(())

    post("/init", "type" -> "doi", "seed" -> seed.toString) {
      status shouldBe 201
      body shouldBe s"Pid type doi is seeded with $seed"
    }
  }

  it should "return a 409 when the DOI is already seeded" in {
    val seed = 123456L
    val otherSeed = 654321L
    app.initialize _ expects(DOI, seed) once() returning Failure(PidAlreadyInitialized(DOI, otherSeed))

    post("/init", "type" -> "doi", "seed" -> seed.toString) {
      status shouldBe 409
      body should include(s"already initialized for a doi")
    }
  }

  it should "return a 500 when the database connection fails unexpectedly" in {
    val seed = 123456L
    app.initialize _ expects(DOI, seed) once() returning Failure(DatabaseException(new SQLException("err")))

    post("/init", "type" -> "doi", "seed" -> seed.toString) {
      status shouldBe 500
      body shouldBe "The database connection failed; cause: err"
    }
  }

  "POST /init?type=doi&seed=abc" should "return a 400 since the seed is not an integer value" in {
    app.initialize _ expects(*, *) never()

    post("/init", "type" -> "doi", "seed" -> "abc") {
      status shouldBe 400
      body shouldBe "The seed is not an integer value"
    }
  }

  "POST /init?type=doi" should "return a 400 since the seed is not provided" in {
    app.initialize _ expects(*, *) never()

    post("/init", "type" -> "doi") {
      status shouldBe 400
      body shouldBe "Usage: POST /init?type={doi|urn}&seed={...}"
    }
  }

  "POST /init?type=unknown" should "return a 400 since the type is not DOI or URN" in {
    app.initialize _ expects(*, *) never()

    post("/init", "type" -> "unknown") {
      status shouldBe 400
      body shouldBe "Usage: POST /init?type={doi|urn}&seed={...}"
    }
  }

  "POST /init?seed=123456" should "return a 400 since the type is not provided" in {
    app.initialize _ expects(*, *) never()

    post("/init", "seed" -> "123456") {
      status shouldBe 400
      body shouldBe "Usage: POST /init?type={doi|urn}&seed={...}"
    }
  }

  "POST /init" should "return a 400 since both type and seed are not provided" in {
    app.initialize _ expects(*, *) never()

    post("/init") {
      status shouldBe 400
      body shouldBe "Usage: POST /init?type={doi|urn}&seed={...}"
    }
  }
}
