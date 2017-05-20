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

import scala.language.postfixOps
import scala.util.Try

trait PidGeneratorComponent {
  this: SeedStorageComponent with PidFormatterComponent =>

  // not a singleton, so no access point

  trait PidGenerator {

    val seedStorage: SeedStorage
    val formatter: PidFormatter

    def next()(implicit connection: Connection): Try[String] = {
      seedStorage.calculateAndPersist(getNextPidNumber).map(formatter.format)
    }

    /**
     * Generates a new PID number from a provided seed. The PID number is then formatted as a DOI or a URN.
     * The PID number also serves as the seed for the next time this function is called (for the same type
     * of identifier). The sequence of PID numbers will go through all the numbers between 0 and 2^31 - 1^,
     * and then return to the first seed. See for proof of this:
     * <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">this page</a>
     */
    private def getNextPidNumber(seed: Long): Option[Long] = {
      val factor = 3 * 7 * 11 * 13 * 23 // = 69069
      val increment = 5
      val modulo = math.pow(2, 31).toLong
      val newSeed = (seed * factor + increment) % modulo

      Option(newSeed).filterNot(seedStorage.firstSeed ==)
    }
  }
}


