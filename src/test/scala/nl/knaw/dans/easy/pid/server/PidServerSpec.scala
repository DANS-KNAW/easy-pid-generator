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

import java.net.ConnectException

import nl.knaw.dans.easy.pid._
import nl.knaw.dans.easy.pid.generator._
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest

import scala.util.{ Failure, Success }

class PidServerSpec extends TestSupportFixture
  with ConfigurationSupportFixture
  with SeedDatabaseFixture
  with ServerTestSupportFixture
  with MockFactory
  with OneInstancePerTest
  with PidServerComponent
  with PidServletComponent
  with URNGeneratorComponent
  with DOIGeneratorComponent
  with SeedStorageComponent
  with DatabaseComponent
  with PidFormatterComponent
  with DatabaseAccessComponent {

  override val database: Database = mock[Database]
  override val urnGenerator: URNGenerator = mock[URNGenerator]
  override val doiGenerator: DOIGenerator = mock[DOIGenerator]
  override val pidServlet: PidServlet = new PidServlet {}
  override val server: PidServer = new PidServer(8060)

  override def afterEach(): Unit = {
    server.stop() shouldBe a[Success[_]]
    server.destroy() shouldBe a[Success[_]]
    super.afterEach()
  }

  "start" should "fire up the server, such that it is available" in {
    server.start() shouldBe a[Success[_]]

    callService() shouldBe successful
  }

  it should "not be available when start isn't called" in {
    // no server start

    a[ConnectException] shouldBe thrownBy(callService())
  }

  "stop" should "terminate the server, such that it is no longer available" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]

    a[ConnectException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server isn't started" in {
    server.stop() shouldBe a[Success[_]]

    a[ConnectException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server was already stopped" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]] // calling stop twice

    a[ConnectException] shouldBe thrownBy(callService())
  }

  "destroy" should "destroy the server when it is already stopped" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]
    server.destroy() shouldBe a[Success[_]]

    a[ConnectException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server wasn't started nor stopped" in {
    server.destroy() shouldBe a[Success[_]]

    a[ConnectException] shouldBe thrownBy(callService())
  }

  it should "fail if the server wasn't already stopped; the service should still be running" in {
    server.start() shouldBe a[Success[_]]
    server.destroy() should matchPattern { case Failure(_: IllegalStateException) => }

    callService() shouldBe successful
  }
}
