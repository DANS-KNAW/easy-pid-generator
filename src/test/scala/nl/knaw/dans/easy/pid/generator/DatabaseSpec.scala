/*
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
package nl.knaw.dans.easy.pid.generator

import java.sql.SQLException

import nl.knaw.dans.easy.pid.PidType.PidType
import nl.knaw.dans.easy.pid.fixture.{ SeedDatabaseFixture, TestSupportFixture }
import nl.knaw.dans.easy.pid.{ PidType, timeZone }
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import resource.managed

import scala.util.{ Failure, Success }

class DatabaseSpec extends TestSupportFixture with SeedDatabaseFixture {

  /**
   * Because of the foreign key constraint on table 'minted' in the database,
   * we have to initialize the seed first, before running certain tests.
   *
   * @param pidType the type of Pid to initialize
   */
  def initSeed(pidType: PidType): Unit = {
    val database: Database = new Database()
    database.initSeed(pidType, 123456) shouldBe a[Success[_]]
  }

  "getSeed" should "return no seed if the database does not contain the given type" in {
    val database: Database = new Database()
    database.getSeed(PidType.URN) should matchPattern { case Success(None) => }
  }

  it should "return the stored seed if the database contains the given type" in {
    val database: Database = new Database()
    managed(connection.prepareStatement("INSERT INTO seed (type, value) VALUES ('urn', '123456');"))
      .foreach(_.executeUpdate())

    database.getSeed(PidType.URN) should matchPattern { case Success(Some(123456L)) => }
  }

  "initSeed" should "insert the given seed and type into the database and return the same seed" in {
    val database: Database = new Database()
    database.initSeed(PidType.DOI, 654321L) shouldBe a[Success[_]]

    database.getSeed(PidType.DOI) should matchPattern { case Success(Some(654321L)) => }
  }

  it should "fail if the seed type is already in the database" in {
    val database: Database = new Database()
    database.initSeed(PidType.DOI, 654321L) shouldBe a[Success[_]]
    inside(database.initSeed(PidType.DOI, 123456L)) {
      case Failure(e: SQLException) =>
        e.getMessage.toLowerCase should (include("unique constraint") and include("seed"))
    }
  }

  "setSeed" should "fail if the seed type is not yet in the database" in {
    val database: Database = new Database()
    database.getSeed(PidType.URN) should matchPattern { case Success(None) => }
    inside(database.setSeed(PidType.URN, 654321L)) {
      case Failure(e: SQLException) =>
        e should have message "Can't update seed for urn as it is not yet defined"
    }
  }

  it should "succeed if the seed type is already in the database, change the seed to the new value and return the new value" in {
    val database: Database = new Database()
    database.initSeed(PidType.URN, 123456L) shouldBe a[Success[_]]
    database.setSeed(PidType.URN, 654321L) shouldBe a[Success[_]]
    database.getSeed(PidType.URN) should matchPattern { case Success(Some(654321L)) => }
  }

  "hasPid" should "return None if the requested PID is not present in the database" in {
    val database: Database = new Database()
    database.hasPid(PidType.URN, "testpid") should matchPattern { case Success(None) => }
  }

  it should "return the related timestamp if the requested PID is already in the database" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created = new DateTime(1992, 7, 30, 16, 2, timeZone)
    val createdFormatted = created.toString(ISODateTimeFormat.dateTimeNoMillis())
      .replace("T", " ")
      .replace("Z", "+0:00")

    initSeed(PidType.URN)

    managed(connection.prepareStatement(s"INSERT INTO minted (type, value, created) VALUES ('urn', '$pid', '$createdFormatted');"))
      .foreach(_.executeUpdate())

    database.hasPid(PidType.URN, pid) should matchPattern { case Success(Some(`created`)) => }
  }

  "addPid" should "insert the given pid, type and timestamp into the database" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created = new DateTime(1992, 7, 30, 16, 2, timeZone)
    initSeed(PidType.DOI)

    database.addPid(PidType.DOI, pid, created) shouldBe a[Success[_]]

    database.hasPid(PidType.DOI, pid) should matchPattern { case Success(Some(`created`)) => }
  }

  it should "succeed if different pids are added with the same type" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created1 = new DateTime(1992, 7, 30, 16, 2, timeZone)
    val created2 = created1.plusDays(1)
    initSeed(PidType.DOI)

    database.addPid(PidType.DOI, pid + 1, created1) shouldBe a[Success[_]]
    database.addPid(PidType.DOI, pid + 2, created2) shouldBe a[Success[_]]

    database.hasPid(PidType.DOI, pid + 1) should matchPattern { case Success(Some(`created1`)) => }
    database.hasPid(PidType.DOI, pid + 2) should matchPattern { case Success(Some(`created2`)) => }
  }

  it should "fail to insert the given pid, type and timestamp when the generator isn't seeded with this type" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created = new DateTime(1992, 7, 30, 16, 2, timeZone)

    inside(database.addPid(PidType.DOI, pid, created)) {
      case Failure(e: SQLException) =>
        e.getMessage.toLowerCase should (include("foreign key") and include("minted"))
    }
  }

  it should "fail if the pid and type are already in the database, if the timestamp is also equal" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created = new DateTime(1992, 7, 30, 16, 2)
    initSeed(PidType.DOI)

    database.addPid(PidType.DOI, pid, created) shouldBe a[Success[_]]
    inside(database.addPid(PidType.DOI, pid, created)) {
      case Failure(e: SQLException) =>
        e.getMessage.toLowerCase should (include("unique constraint") and include("minted"))
    }
  }

  it should "fail if the pid and type are already in the database, even if the timestamp is different" in {
    val database: Database = new Database()
    val pid = "testpid"
    val created = new DateTime(1992, 7, 30, 16, 2)
    initSeed(PidType.DOI)

    database.addPid(PidType.DOI, pid, created) shouldBe a[Success[_]]
    inside(database.addPid(PidType.DOI, pid, created.plusDays(1))) {
      case Failure(e: SQLException) =>
        e.getMessage.toLowerCase should (include("unique constraint") and include("minted"))
    }
  }
}
