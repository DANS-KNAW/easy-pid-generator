package nl.knaw.dans.easy.pid.service

import nl.knaw.dans.easy.pid.generator._
import nl.knaw.dans.easy.pid.{ DatabaseAccessComponent, PropertiesSupportFixture, SeedDatabaseFixture, ServerTestSupportFixture }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest

import scala.util.{ Failure, Success }

class PidServerSpec extends PropertiesSupportFixture with SeedDatabaseFixture with ServerTestSupportFixture with MockFactory with OneInstancePerTest
  with PidServerComponent
  with ServletMounterComponent
  with PidServletComponent
  with DOIGeneratorWiring
  with URNGeneratorWiring
  with SeedStorageComponent
  with DatabaseComponent
  with PidFormatterComponent
  with DatabaseAccessComponent
  with DebugEnhancedLogging {

  val database: Database = mock[Database]
  val pidServlet: PidServlet = new PidServlet {}
  val mounter: ServletMounter = new ServletMounter {}
  val server: PidServer = PidServer(8060)

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

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  "stop" should "terminate the server, such that it is no longer available" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server isn't started" in {
    server.stop() shouldBe a[Success[_]]

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server was already stopped" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]] // calling stop twice

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  "destroy" should "destroy the server when it is already stopped" in {
    server.start() shouldBe a[Success[_]]
    server.stop() shouldBe a[Success[_]]
    server.destroy() shouldBe a[Success[_]]

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  it should "do nothing when the server wasn't started nor stopped" in {
    server.destroy() shouldBe a[Success[_]]

    a[RuntimeException] shouldBe thrownBy(callService())
  }

  it should "fail if the server wasn't already stopped; the service should still be running" in {
    server.start() shouldBe a[Success[_]]
    server.destroy() should matchPattern { case Failure(_: IllegalStateException) => }

    callService() shouldBe successful
  }
}
