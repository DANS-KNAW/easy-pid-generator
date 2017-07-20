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
package nl.knaw.dans.easy.pid.seedstorage

import java.sql.{ Connection, SQLException }

import nl.knaw.dans.easy.pid.PidType
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

import scala.util.{ Failure, Success, Try }

class Database extends DebugEnhancedLogging {

  def getSeed(pidType: PidType)(implicit connection: Connection): Try[Option[Long]] = {
    trace(pidType)

    val resultSet = for {
      prepStatement <- managed(connection.prepareStatement("SELECT value FROM seed WHERE type=?;"))
      _ = prepStatement.setString(1, pidType.name)
      resultSet <- managed(prepStatement.executeQuery())
    } yield resultSet

    resultSet.map(Option(_).filter(_.next()).map(_.getLong("value"))).tried
  }

  def initSeed(pidType: PidType, seed: Long)(implicit connection: Connection): Try[Long] = {
    trace(pidType, seed)

    managed(connection.prepareStatement("INSERT INTO seed (type, value) VALUES (?, ?);"))
      .map(prepStatement => {
        prepStatement.setString(1, pidType.name)
        prepStatement.setLong(2, seed)
        prepStatement.executeUpdate()
      })
      .tried
      .map(_ => seed)
  }

  def setSeed(pidType: PidType, seed: Long)(implicit connection: Connection): Try[Long] = {
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
        case _ => Success(seed)
      }
  }
}
