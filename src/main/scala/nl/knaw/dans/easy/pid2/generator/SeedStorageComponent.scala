package nl.knaw.dans.easy.pid2.generator

import nl.knaw.dans.easy.pid2.{ DatabaseAccess, PidType, RanOutOfSeeds }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Try }

trait SeedStorageComponent extends DebugEnhancedLogging {

  type FirstSeed = Long
  val seedStorage: SeedStorage

  trait SeedStorage {
    val firstSeedMap: Map[PidType, FirstSeed]
    val database: Database
    val databaseAccess: DatabaseAccess

    /**
     * Calculates the next PID seed from the previously stored one and makes
     * sure that it is persisted. Returns a Failure if there is no next PID seed or
     * if the new seed could not be persisted
     */
    def calculateAndPersist(pidType: PidType)(nextPid: Long => Long): Try[Long] = {
      databaseAccess.doTransaction(implicit connection => {
        val firstSeed = firstSeedMap(pidType)
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
      })
    }
  }

  object SeedStorage {
    def apply(firstSeeds: Map[PidType, FirstSeed])(db: Database, dbAccess: DatabaseAccess): SeedStorage = {
      new SeedStorage {
        val firstSeedMap: Map[PidType, FirstSeed] = firstSeeds
        val database: Database = db
        val databaseAccess: DatabaseAccess = dbAccess
      }
    }
  }
}
