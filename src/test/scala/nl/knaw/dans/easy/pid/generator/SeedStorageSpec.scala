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

import java.sql.Connection

import nl.knaw.dans.easy.pid.{ DOI, PidType, RanOutOfSeeds, TestSupportFixture }
import org.scalamock.scalatest.MockFactory

import scala.language.postfixOps
import scala.util.{ Failure, Success }

class SeedStorageSpec extends TestSupportFixture
  with MockFactory
  with SeedStorageComponent
  with DatabaseComponent {

  implicit val connection: Connection = mock[Connection]
  override val database: Database = mock[Database]
  val seedStorage: SeedStorage = new SeedStorage {
    override val pidType: PidType = DOI
    override val firstSeed: Long = 654321
  }

  "calculateAndPersist" should "succeed by returning the next PID after persisting this new one" in {
    val previousPid = 123456L
    val nextPid = 123457L

    (database.getSeed(_: PidType)(_: Connection)) expects(DOI, connection) once() returning Success(Some(previousPid))
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, connection) never()
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(DOI, nextPid, connection) once() returning Success(nextPid)

    seedStorage.calculateAndPersist(1 +) should matchPattern { case Success(`nextPid`) => }
  }

  it should "fail if there is no next PID" in {
    val previousPid = 123456L

    (database.getSeed(_: PidType)(_: Connection)) expects(DOI, connection) once() returning Success(Some(previousPid))
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, connection) never()
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, connection) never()

    seedStorage.calculateAndPersist(_ => seedStorage.firstSeed) should matchPattern { case Failure(RanOutOfSeeds(DOI)) => }
  }

  it should "succeed by initializing, persisting and returning the first seed" in {
    val next = mock[Long => Long]
    val first = seedStorage.firstSeed

    (database.getSeed(_: PidType)(_: Connection)) expects(DOI, connection) once() returning Success(None)
    (database.initSeed(_: PidType, _: Long)(_: Connection)) expects(DOI, first, connection) once() returning Success(first)
    (database.setSeed(_: PidType, _: Long)(_: Connection)) expects(*, *, connection) never()
    next.apply _ expects * never()

    seedStorage.calculateAndPersist(next) should matchPattern { case Success(`first`) => }
  }
}
