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

import nl.knaw.dans.easy.pid.Seed

class PidFormatter(namespace: String, dashPosition: Int, illegalChars: Map[Char, Char], length: Int) {

  private val maxRadius = 36

  def format(seed: Seed): String = {
    PidFormatter.format(
      prefix = namespace,
      radix = maxRadius - illegalChars.size,
      len = length,
      charMap = illegalChars,
      dashPos = dashPosition
    )(seed)
  }
}

object PidFormatter {

  /**
   * Formats a PID using the specs provided in the parameters.
   *
   * @param prefix  the prefix to use
   * @param radix   the base of the number system to use, e.g., base 32 for a
   *                number system of 32 digits. The digits 0..9 will be used first and then
   *                a..z. The maximum radix therefore is 36
   * @param len     the length that the result should have
   * @param charMap mapping for forbidden chars. The forbidden chars should
   *                be mapped to chars normally not used with the given radix. The radix
   *                should therefore be sufficiently small to have enough unused chars.
   * @param dashPos position to insert a dash for readability
   * @param seed    the seed to format
   * @return the formatted PID
   */
  def format(prefix: String, radix: Int, len: Int, charMap: Map[Char, Char], dashPos: Int)(seed: Seed): String =
    prefix + insertDashAt(dashPos)(convertToString(seed, radix, len, charMap))

  def convertToString(seed: Seed, radix: Int, length: Int, illegalCharMap: Map[Char, Char] = Map()): String = {
    // formats the seed in base `radix`, pads it with 0's and removes any illegal characters
    s"%${ length }s"
      .format(java.lang.Long.toString(seed, radix).toLowerCase)
      .replace(' ', '0')
      .map(c => illegalCharMap.getOrElse(c, c))
  }

  def insertDashAt(i: Int)(s: String): String = {
    val (prefix, suffix) = s.splitAt(i)
    s"$prefix-$suffix"
  }
}
