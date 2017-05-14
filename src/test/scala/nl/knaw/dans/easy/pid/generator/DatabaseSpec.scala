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
package nl.knaw.dans.easy.pid.generator

import java.sql.SQLException

import nl.knaw.dans.easy.pid.{ DOI, SeedDatabaseFixture, URN }
import resource.managed

import scala.util.{ Failure, Success }

class DatabaseSpec extends SeedDatabaseFixture with DatabaseComponent {
  val database: Database = new Database {}

  "getSeed" should "return no seed if the database does not contain the given type" in {
    database.getSeed(URN) should matchPattern { case Success(None) => }
  }

  it should "return the stored seed if the database contains the given type" in {
    managed(connection.prepareStatement("INSERT INTO seed (type, value) VALUES ('urn', '123456');"))
      .foreach(_.executeUpdate())

    database.getSeed(URN) should matchPattern { case Success(Some(123456L)) => }
  }

  "initSeed" should "insert the given seed and type into the database and return the same seed" in {
    database.initSeed(DOI, 654321L) should matchPattern { case Success(654321L) => }

    database.getSeed(DOI) should matchPattern { case Success(Some(654321L)) => }
  }

  it should "fail if the seed type is already in the database" in {
    database.initSeed(DOI, 654321L) shouldBe a[Success[_]]
    inside(database.initSeed(DOI, 123456L)) {
      case Failure(e: SQLException) =>
        e should have message "[SQLITE_CONSTRAINT_UNIQUE]  A UNIQUE constraint failed (UNIQUE constraint failed: seed.type)"
    }
  }

  "setSeed" should "fail if the seed type is not yet in the database" in {
    database.getSeed(URN) should matchPattern { case Success(None) => }
    inside(database.setSeed(URN, 654321L)) {
      case Failure(e: SQLException) =>
        e should have message "Can't update seed for URN as it is not yet defined"
    }
  }

  it should "succeed if the seed type is already in the database, change the seed to the new value and return the new value" in {
    database.initSeed(URN, 123456L) shouldBe a[Success[_]]
    database.setSeed(URN, 654321L) should matchPattern { case Success(654321L) => }
    database.getSeed(URN) should matchPattern { case Success(Some(654321L)) => }
  }
}
