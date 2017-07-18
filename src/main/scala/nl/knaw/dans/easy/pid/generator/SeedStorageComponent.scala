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

import nl.knaw.dans.easy.pid.{ PidType, RanOutOfSeeds }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Try }

trait SeedStorageComponent extends DebugEnhancedLogging {
  this: DatabaseComponent =>

  // not a singleton, so no access point

  trait SeedStorage {

    val pidType: PidType
    val firstSeed: Long

    /**
     * Calculates the next PID seed from the previously stored one and makes
     * sure that it is persisted. Returns a Failure if there is no next PID seed or
     * if the new seed could not be persisted
     */
    def calculateAndPersist(nextPid: Long => Long)(implicit connection: Connection): Try[Long] = {
      database.getSeed(pidType)
        .flatMap {
          case Some(seed) =>
            nextPid(seed) match {
              case `firstSeed` => Failure(RanOutOfSeeds(pidType))
              case nextSeed => database.setSeed(pidType, nextSeed)
            }
          case None =>
            logger.warn(s"No previous PID found. This should only happen once. Initializing with initial seed for $pidType")
            logger.info(s"Initializing seed with value $firstSeed")
            database.initSeed(pidType, firstSeed)
        }
    }
  }
}
