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

import java.util.concurrent.{ ConcurrentHashMap, CountDownLatch, Executors }

import nl.knaw.dans.easy.pid.fixture.{ ConfigurationSupportFixture, SeedDatabaseFixture, TestSupportFixture }
import nl.knaw.dans.easy.pid.generator.Database
import nl.knaw.dans.lib.error.TryExtensions
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{ Failure, Success, Try }

class PidGeneratorAppSpec extends TestSupportFixture
  with SeedDatabaseFixture
  with ConfigurationSupportFixture {

  val app = new PidGeneratorApp(configuration)

  override def beforeEach(): Unit = {
    super.beforeEach()
    app.init().unsafeGetOrThrow
  }

  override def afterEach(): Unit = {
    app.close()
    super.afterEach()
  }

  "exists(doi)" should "return true when it is already in the database" in {
    val database = new Database
    val seed = 1073741824L
    val doi = "10.5072/dans-x6f-kf66"

    // init seed
    database.initSeed(PidType.DOI, seed) shouldBe a[Success[_]]

    // generate DOI
    app.generate(PidType.DOI) should matchPattern { case Success(`doi`) => }

    // test that the next seed and new DOI are stored in the database
    app.exists(PidType.DOI, doi) should matchPattern { case Success(true) => }
  }

  it should "return false when it is already in the database" in {
    val doi = "10.5072/dans-x6f-kf66"

    // do not initialize seed
    // do not generate a DOI

    // test that the next seed and new DOI are stored in the database
    app.exists(PidType.DOI, doi) should matchPattern { case Success(false) => }
  }

  "generate(doi)" should "return the next DOI and store it in the database as well" in {
    val database = new Database
    val seed = 1073741824L
    val doi = "10.5072/dans-x6f-kf66"

    // init seed
    database.initSeed(PidType.DOI, seed) shouldBe a[Success[_]]

    // generate DOI
    app.generate(PidType.DOI) should matchPattern { case Success(`doi`) => }

    // test that the next seed and new DOI are stored in the database
    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(1073741829L)) => }
    database.hasPid(PidType.DOI, doi) shouldBe a[Success[_]]
  }

  it should "fail when the DOI's seed has never been initialized" in {
    app.generate(PidType.DOI) should matchPattern { case Failure(PidNotInitialized(PidType.DOI)) => }
  }

  it should "fail when the DOI already exists" in {
    val database = new Database
    val seed = 1073741824L
    val doi = "10.5072/dans-x6f-kf66"
    val timestamp = DateTime.now(timeZone)

    // init seed
    database.initSeed(PidType.DOI, seed) shouldBe a[Success[_]]
    database.addPid(PidType.DOI, doi, timestamp) shouldBe a[Success[_]]

    // generate DOI
    app.generate(PidType.DOI) should matchPattern { case Failure(DuplicatePid(PidType.DOI, `seed`, 1073741829L, `doi`, `timestamp`)) => }

    // test the seed is not updated
    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(`seed`)) => }
  }

  it should "generate the second DOI" in {
    val database = new Database
    val seed = 1073741824L
    val doi1 = "10.5072/dans-x6f-kf66"
    val doi2 = "10.5072/dans-x6g-x2hb"

    // init seed
    database.initSeed(PidType.DOI, seed) shouldBe a[Success[_]]

    // generate DOI
    app.generate(PidType.DOI) should matchPattern { case Success(`doi1`) => }
    app.generate(PidType.DOI) should matchPattern { case Success(`doi2`) => }

    // test that the next seed and new DOI are stored in the database
    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(1074087174)) => }
    database.hasPid(PidType.DOI, doi1) shouldBe a[Success[_]]
    database.hasPid(PidType.DOI, doi2) shouldBe a[Success[_]]
  }

  it should "manage concurrency correctly" in {
    app.initialize(PidType.DOI, 123456)

    def test(name: String, start: CountDownLatch, done: CountDownLatch,
             results: mutable.Map[String, Try[Pid]]): Runnable = () => {
      start.await()
      results.put(name, app.generate(PidType.DOI))
      done.countDown()
    }

    val n = 10
    val ex = Executors.newFixedThreadPool(10)
    val start = new CountDownLatch(1)
    val done = new CountDownLatch(n)
    val results = new ConcurrentHashMap[String, Try[Pid]]().asScala

    for (i <- 1 to n) {
      ex.execute { test(s"test$i", start, done, results) }
    }
    start.countDown()
    done.await()

    val expectedResults = Set(
      "10.5072/dans-2ap-4qfd",
      "10.5072/dans-zve-22y5",
      "10.5072/dans-x5f-3p9r",
      "10.5072/dans-xnr-c7jf",
      "10.5072/dans-x75-qa68",
      "10.5072/dans-229-ftfq",
      "10.5072/dans-zyq-5znp",
      "10.5072/dans-z8c-gnm3",
      "10.5072/dans-2zt-warx",
      "10.5072/dans-z7p-ebra",
    )

    results.keys should contain theSameElementsAs (1 to n).map(i => s"test$i")
    all(results.values) shouldBe a[Success[_]]
    results.collect { case (_, Success(doi)) => doi }.toSeq should contain theSameElementsAs expectedResults
  }

  "initialize" should "set a seed in the database" in {
    val database = new Database
    val seed = 1073741824L

    // initialize seed
    app.initialize(PidType.DOI, seed) shouldBe a[Success[_]]

    // test that seed is in the database
    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(`seed`)) => }
  }

  it should "fail when the seed is already set" in {
    val database = new Database
    val seed = 1073741824L
    val otherSeed = 4281473701L

    // initialize seed twice
    app.initialize(PidType.DOI, seed) shouldBe a[Success[_]]
    app.initialize(PidType.DOI, otherSeed) should matchPattern { case Failure(PidAlreadyInitialized(PidType.DOI, `seed`)) => }

    // test that the original seed still stands
    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(`seed`)) => }
  }
}
