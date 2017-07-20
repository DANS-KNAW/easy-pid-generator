package nl.knaw.dans.easy.pid2.generator

import nl.knaw.dans.easy.pid2.PidType

import scala.util.Try

trait PidGeneratorComponent {
  this: SeedStorageComponent =>

  val pidGenerator: PidGenerator

  class PidGenerator(formatters: Map[PidType, PidFormatter]) {

    def generate(pidType: PidType): Try[String] = {
      seedStorage.calculateAndPersist(pidType)(getNextPidNumber).map(formatters(pidType).format)
    }

    /**
     * Generates a new PID number from a provided seed. The PID number is then formatted as a DOI or a URN.
     * The PID number also serves as the seed for the next time this function is called (for the same type
     * of identifier). The sequence of PID numbers will go through all the numbers between 0 and 2^31 - 1^,
     * and then return to the first seed. See for proof of this:
     * <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">this page</a>
     */
    private def getNextPidNumber(seed: Long): Long = {
      val factor = 3 * 7 * 11 * 13 * 23 // = 69069
      val increment = 5
      val modulo = math.pow(2, 31).toLong

      (seed * factor + increment) % modulo
    }
  }
}
