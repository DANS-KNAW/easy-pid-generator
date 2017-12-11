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

import java.sql.{ Connection, SQLException }

import nl.knaw.dans.easy.pid.{ Pid, PidType, Seed, _ }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import resource.managed

import scala.util.{ Failure, Success, Try }

trait DatabaseComponent extends DebugEnhancedLogging {

  val database: Database

  trait Database {

    def getSeed(pidType: PidType)(implicit connection: Connection): Try[Option[Seed]] = {
      trace(pidType)
      val resultSet = for {
        prepStatement <- managed(connection.prepareStatement("SELECT value FROM seed WHERE type=?;"))
        _ = prepStatement.setString(1, pidType.name)
        resultSet <- managed(prepStatement.executeQuery())
      } yield resultSet

      resultSet.map(Option(_).withFilter(_.next()).map(_.getLong("value"))).tried
    }

    def initSeed(pidType: PidType, seed: Seed)(implicit connection: Connection): Try[Unit] = {
      trace(pidType, seed)

      managed(connection.prepareStatement("INSERT INTO seed (type, value) VALUES (?, ?);"))
        .map(prepStatement => {
          prepStatement.setString(1, pidType.name)
          prepStatement.setLong(2, seed)
          prepStatement.executeUpdate()
        })
        .tried
        .map(_ => ())
    }

    def setSeed(pidType: PidType, seed: Seed)(implicit connection: Connection): Try[Unit] = {
      trace(pidType, seed)

      managed(connection.prepareStatement("UPDATE seed SET value=? WHERE type=?;"))
        .map(prepStatement => {
          prepStatement.setLong(1, seed)
          prepStatement.setString(2, pidType.name)
          prepStatement.executeUpdate()
        })
        .tried
        .flatMap {
          case 0 => Failure(new SQLException(s"Can't update seed for $pidType as it is not yet defined"))
          case _ => Success(())
        }
    }

    def hasPid(pidType: PidType, pid: Pid)(implicit connection: Connection): Try[Option[DateTime]] = {
      trace(pidType, pid)

      val query = "SELECT created FROM minted WHERE type=? AND value=?;"
      val resultSet = for {
        prepStatement <- managed(connection.prepareStatement(query))
        _ = prepStatement.setString(1, pidType.name)
        _ = prepStatement.setString(2, pid)
        resultSet <- managed(prepStatement.executeQuery())
      } yield resultSet

      resultSet.map {
        Option(_)
          .withFilter(_.next())
          .map(result => new DateTime(result.getTimestamp("created", timeZone), timeZone))
      }.tried
    }

    def addPid(pidType: PidType, pid: Pid, created: DateTime)(implicit connection: Connection): Try[Unit] = {
      trace(pidType, pid, created)

      managed(connection.prepareStatement("INSERT INTO minted (type, value, created) VALUES (?, ?, ?);"))
        .map(prepStatement => {
          prepStatement.setString(1, pidType.name)
          prepStatement.setString(2, pid)
          prepStatement.setTimestamp(3, created, timeZone)
          prepStatement.executeUpdate()
        })
        .tried
        .map(_ => ())
    }
  }
}
