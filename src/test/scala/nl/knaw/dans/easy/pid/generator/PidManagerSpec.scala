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

import nl.knaw.dans.easy.pid._
import nl.knaw.dans.easy.pid.fixture.{ SeedDatabaseFixture, TestSupportFixture }
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class PidManagerSpec extends TestSupportFixture
  with MockFactory
  with SeedDatabaseFixture {

  val database: Database = mock[Database]
  val formatter: PidFormatter = mock[PidFormatter]
  val pidManager: PidManager = new PidManager(Map(PidType.DOI -> formatter), database)

  "exists" should "return true when the given pid already exists in the database" in {
    val pid = "da_pid"

    database.hasPid _ expects(PidType.DOI, pid) once() returning Success(Some(DateTime.now()))

    pidManager.exists(PidType.DOI, pid) should matchPattern { case Success(true) => }
  }

  it should "return false when the given pid doesn't exist in the database" in {
    val pid = "da_pid"

    database.hasPid _ expects(PidType.DOI, pid) once() returning Success(None)

    pidManager.exists(PidType.DOI, pid) should matchPattern { case Success(false) => }
  }

  "generate" should "return a new Pid with a given PidType, while calculating/storing the next seed and storing the new Pid" in {
    val seed = 123456L
    val nextSeed = 2084531525L
    val pid = "da_pid"

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(seed))
      formatter.format _ expects nextSeed once() returning pid
      database.hasPid _ expects(PidType.DOI, pid) once() returning Success(None)
      database.setSeed _ expects(PidType.DOI, nextSeed) once() returning Success(())
      database.addPid _ expects(PidType.DOI, pid, *) once() returning Success(())
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Success(`pid`) => }
  }

  it should "fail immediately when the seed could not be retrieved, without doing any other queries or calculations" in {
    val e = new SQLException("msg")

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Failure(e)
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }

  it should "fail immediately when the seed was retrieved successfully, but was None" in {
    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(None)
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(PidNotInitialized(PidType.DOI)) => }
  }

  it should "fail when the Pid existence check fails" in {
    val seed = 123456L
    val nextSeed = 2084531525L
    val pid = "da_pid"
    val e = new SQLException("msg")

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(seed))
      formatter.format _ expects nextSeed once() returning pid
      database.hasPid _ expects(PidType.DOI, pid) once() returning Failure(e)
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }

  it should "fail when the newly calculated Pid already exists, without storing the new seed" in {
    val seed = 123456L
    val nextSeed = 2084531525L
    val pid = "da_pid"
    val timestamp = new DateTime(1992, 7, 30, 16, 1, 2)

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(seed))
      formatter.format _ expects nextSeed once() returning pid
      database.hasPid _ expects(PidType.DOI, pid) once() returning Success(Some(timestamp))
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(DuplicatePid(PidType.DOI, `seed`, `nextSeed`, `pid`, `timestamp`)) => }
  }

  it should "fail when the new seed could not be stored properly, without storing the newly generated Pid" in {
    val seed = 123456L
    val nextSeed = 2084531525L
    val pid = "da_pid"
    val e = new SQLException("msg")

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(seed))
      formatter.format _ expects nextSeed once() returning pid
      database.hasPid _ expects(PidType.DOI, pid) once() returning Success(None)
      database.setSeed _ expects(PidType.DOI, nextSeed) once() returning Failure(e)
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }

  it should "fail when the newly generated Pid could not be stored" in {
    val seed = 123456L
    val nextSeed = 2084531525L
    val pid = "da_pid"
    val e = new SQLException("msg")

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(seed))
      formatter.format _ expects nextSeed once() returning pid
      database.hasPid _ expects(PidType.DOI, pid) once() returning Success(None)
      database.setSeed _ expects(PidType.DOI, nextSeed) once() returning Success(())
      database.addPid _ expects(PidType.DOI, pid, *) once() returning Failure(e)
    }

    pidManager.generate(PidType.DOI) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }

  "initialize" should "set the initial seed in the database when it isn't set already" in {
    val seed = 123456L

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(None)
      database.initSeed _ expects(PidType.DOI, seed) once() returning Success(())
    }

    pidManager.initialize(PidType.DOI, seed) shouldBe a[Success[_]]
  }

  it should "fail when the seed is already set" in {
    val seed = 123456L
    val otherSeed = 654321L

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(Some(otherSeed))
      database.initSeed _ expects(*, *) never()
    }

    pidManager.initialize(PidType.DOI, seed) should matchPattern {
      case Failure(PidAlreadyInitialized(PidType.DOI, `otherSeed`)) =>
    }
  }

  it should "fail when the database connection fails unexpectedly for getSeed" in {
    val e = new SQLException("msg")
    val seed = 123456L

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Failure(e)
      database.initSeed _ expects(*, *) never()
    }

    pidManager.initialize(PidType.DOI, seed) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }

  it should "fail when the database connection fails unexpectedly for initSeed" in {
    val e = new SQLException("msg")
    val seed = 123456L

    inSequence {
      database.getSeed _ expects PidType.DOI once() returning Success(None)
      database.initSeed _ expects(PidType.DOI, seed) once() returning Failure(e)
    }

    pidManager.initialize(PidType.DOI, seed) should matchPattern { case Failure(DatabaseException(`e`)) => }
  }
}
