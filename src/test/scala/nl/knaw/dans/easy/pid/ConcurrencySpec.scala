package nl.knaw.dans.easy.pid

import java.util.concurrent.{ ConcurrentHashMap, CountDownLatch }

import nl.knaw.dans.easy.pid.fixture.{ ConfigurationSupportFixture, SeedDatabaseFixture, TestSupportFixture }
import nl.knaw.dans.easy.pid.generator.DatabaseComponent

import scala.collection.JavaConverters._
import scala.util.{ Success, Try }

class ConcurrencySpec extends TestSupportFixture
  with SeedDatabaseFixture
  with ConfigurationSupportFixture
  with DatabaseComponent {

  val database: Database = new Database {}
  val app = new PidGeneratorApp(new ApplicationWiring(configuration))

  override def beforeEach(): Unit = {
    super.beforeEach()
    app.init().unsafeGetOrThrow
  }

  override def afterEach(): Unit = {
    app.close()
    super.afterEach()
  }

  "generate" should "manage concurrency correctly" in {
    app.initialize(DOI, 123456)

    val results = new ConcurrentHashMap[String, Try[Pid]]().asScala

    def test(name: String, latch: CountDownLatch, done: CountDownLatch): Runnable = new Runnable {
      def run(): Unit = {
        latch.await()
        results.put(name, app.generate(DOI))
        done.countDown()
      }
    }

    val n = 10
    val latch = new CountDownLatch(1)
    val done = new CountDownLatch(n)

    for (i <- 1 to n) {
      new Thread(test(s"test$i", latch, done)).start()
    }

    latch.countDown()
    done.await()

    results.keys should contain theSameElementsAs (1 to n).map(i => s"test$i")
    all(results.values).shouldBe(a[Success[_]])
  }
}
