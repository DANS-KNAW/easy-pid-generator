package nl.knaw.dans.easy

import Math.pow
import java.io.File
import com.typesafe.config.ConfigFactory

package object pid {
  val MAX_RADIX = 36

  /**
   * Formats a PID using the specs provided in the parameters.
   *
   * @param prefix the prefix to use
   * @param radix the base of the number system to use, e.g., base 32 for a
   *   number system of 32 digits. The digits 0..9 will be used first and then
   *   a..z. The maximum radix therefore is 36
   * @param len the length that the result should have
   * @param charMap mapping for forbidden chars. The forbidden chars should
   *   be mapped to chars normally not used with the given radix. The radix 
   *   should therefore be sufficiently small to have enough unused chars.
   * @param dashPos position to insert a dash for readability
   * @param pid the PID number to format
   * @returns the formatted PID 
   */
  def format(prefix: String, radix: Int, len: Int, charMap: Map[Char, Char], dashPos: Int)(pid: Long): String =
    prefix + insertDashAt(dashPos)(convertToString(pid, radix, len, charMap))

  def convertToString(pid: Long, radix: Int, length: Int, illegalCharMap: Map[Char, Char] = Map()) = {
    def padWithZeroes(s: String) = String.format(s"%${length}s", s).replace(' ', '0')
    padWithZeroes(java.lang.Long.toString(pid, radix).toLowerCase).map { c => illegalCharMap.getOrElse(c, c) }
  }

  def insertDashAt(i: Int)(s: String): String = s.substring(0, i) + "-" + s.substring(i)
}