/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid

import java.io.File
import java.lang.Math.pow

import com.typesafe.config.Config

import scala.util.Try

case class PidGenerator(seed: SeedStorage, firstSeed: Long, format: Long => String) {

  def next(): Try[String] = seed.calculateAndPersist(getNextPidNumber).map(format)

  /**
   * Generates a new PID number from a provided seed. The PID number is then formatted as a DOI or a URN.
   * The PID number also serves as the seed for the next time this function is called (for the same type
   * of identifier). The sequence of PID numbers will go through all the numbers between 0 and 2^31 - 1^,
   * and then return to the first seed. See for proof of this:
   * <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">this page</a>
   */
  def getNextPidNumber(seed: Long): Option[Long] = {
    val factor = 3 * 7 * 11 * 13 * 23 // = 69069
    val increment = 5
    val modulo = pow(2, 31).toLong
    val newSeed = (seed * factor + increment) % modulo

    if (newSeed == firstSeed) Option.empty
    else Option(newSeed)
  }
}

object PidGenerator {
  private def generate(key: String, length: Int, conf: Config, home: File, illegalChars: Map[Char, Char]): PidGenerator = {
    val firstSeed = conf.getLong(s"types.$key.firstSeed")
    val storage = DbBasedSeedStorage(key, firstSeed, new File(home, "cfg/hibernate.conf.xml"))
    def formatter(pid: Long) = format(
      prefix = conf.getString(s"types.$key.namespace"),
      radix = MAX_RADIX - illegalChars.size,
      len = length,
      charMap = illegalChars,
      dashPos = conf.getInt(s"types.$key.dashPosition")
    )(pid)

    PidGenerator(storage, firstSeed, formatter)
  }

  def urnGenerator(conf: Config, home: File): PidGenerator = {
    generate("urn", 6, conf, home, Map.empty)
  }

  def doiGenerator(conf: Config, home: File): PidGenerator = {
    generate("doi", 7, conf, home, Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v'))
  }
}
