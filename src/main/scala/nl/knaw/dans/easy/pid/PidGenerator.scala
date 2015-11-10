package nl.knaw.dans.easy.pid

import java.lang.Math.pow
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.util.Success
import scala.util.Success
import scala.util.Success

case class PidGenerator(seed: SeedStorage, firstSeed: Long, format: Long => String) {
  def next(): Try[String] =
    seed.calculateAndPersist(getNextPidNumber).map(format(_))


  /**
   * Generates a new PID number from a provided seed. The PID number is then formatted as a DOI or a URN.
   * The PID number also serves as the seed for the next time this function is called (for the same type
   * of identifier). The sequence of PID numbers will go through all the numbers between 0 and 2^31 - 1,
   * and then return to the first seed. See for proof of this:
   * <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">this page</a>
   */
  def getNextPidNumber(seed: Long): Option[Long] = {
    val factor = 3 * 7 * 11 * 13 * 23 // = 69069
    val increment = 5
    val modulo = pow(2, 31).toLong
    val newSeed = (seed * factor + increment) % modulo
    if (newSeed == firstSeed) None
    else Some(newSeed)
  }
}
