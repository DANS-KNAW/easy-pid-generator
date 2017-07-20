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
package nl.knaw.dans.easy.pid2.generator

import java.sql.Connection

import nl.knaw.dans.easy.pid2._
import org.scalamock.scalatest.MockFactory

import scala.language.postfixOps
import scala.util.{ Failure, Success }

class SeedStorageSpec extends TestSupportFixture with MockFactory with SeedDatabaseFixture with SeedStorageComponent {

  val database: Database = mock[Database]
  val initSeed = 654321L
  val seedStorage: SeedStorage = SeedStorage(Map(DOI -> initSeed))(database, databaseAccess)

  "calculateAndPersist" should "succeed by returning the next PID after persisting this new one" in {
    val previousPid = 123456L
    val nextPid = 123457L

    (database.getSeed(_: PidType)(_: Connection)) expects (DOI, *) once() returning Success(Some(previousPid))
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, *) never()
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(DOI, nextPid, *) once() returning Success(nextPid)

    seedStorage.calculateAndPersist(DOI)(1 +) should matchPattern { case Success(`nextPid`) => }
  }

  it should "fail if there is no next PID" in {
    val previousPid = 123456L

    (database.getSeed(_: PidType)(_: Connection)) expects (DOI, *) once() returning Success(Some(previousPid))
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, *) never()
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, *) never()

    seedStorage.calculateAndPersist(DOI)(_ => initSeed) should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }
  }

  it should "succeed by initializing, persisting and returning the first seed" in {
    val next = mock[Long => Long]

    (database.getSeed(_: PidType)(_: Connection)) expects (DOI, *) once() returning Success(None)
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(DOI, initSeed, *) once() returning Success(initSeed)
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, *) never()
    next.apply _ expects * never()

    seedStorage.calculateAndPersist(DOI)(next) should matchPattern { case Success(`initSeed`) => }
  }
}
