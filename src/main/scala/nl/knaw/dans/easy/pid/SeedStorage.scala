package nl.knaw.dans.easy.pid

import java.io.File
import java.lang.Thread._
import java.net.SocketException
import javax.persistence.Entity

import org.hibernate.HibernateException

import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.exception.JDBCConnectionException
import org.hibernate.exception.GenericJDBCException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

sealed trait SeedStorage {
  /**
   * Calculates the next PID seed from the previously stored one and makes
   * sure that it is persisted. Returns a Failure if there is no next PID seed or
   * if the new seed could not be persisted
   */
  def calculateAndPersist(f: Long => Option[Long]): Try[Long]
}

@Entity
class Seed() {
  @BeanProperty
  var pidType: String = null

  @BeanProperty
  var value: Long = Long.MinValue
}

case class RanOutOfSeeds() extends Exception

case class DbBasedSeedStorage(key: String, first: Long, hibernateConfig: File) extends SeedStorage {
  val log = LoggerFactory.getLogger(classOf[DbBasedSeedStorage])
  val conf = new Configuration().configure(hibernateConfig)
  val serviceRegistry = new StandardServiceRegistryBuilder().applySettings(conf.getProperties).build()

  var sessionFactory = conf.buildSessionFactory(serviceRegistry)

  override def calculateAndPersist(f: Long => Option[Long]): Try[Long] = {

    //@tailrec
    def iterWhileRestarting(timeout: Int = 0, maxRetry: Int = 3): Try[Long] = {
      val session = sessionFactory.getCurrentSession
      session.beginTransaction()
      try {
        session.get(classOf[Seed], key) match {
          case seed: Seed =>
            f(seed.value) match {
              case Some(next) =>
                val seed = new Seed
                seed.pidType = key
                seed.value = next
                session.merge(seed)
                session.getTransaction.commit()
                Success(next)
              case None => Failure(RanOutOfSeeds())
            }
          case _ =>
            log.warn("NO PREVIOUS PID FOUND. THIS SHOULD ONLY HAPPEN ONCE!! INITIALIZING WITH INITIAL SEED FOR {}", key)
            log.info("Initializing seed with value {}", first)
            val seed = new Seed
            seed.pidType = key
            seed.value = first
            session.save(seed)
            session.getTransaction.commit()
            Success(first)
        }
      } catch {
        case e: GenericJDBCException if e.getCause.isInstanceOf[PSQLException] =>
          val msg = s"""Database server connection lost
                       |GenericJDBCException ${e.getMessage}
                       |PSQLException ${e.getCause.getMessage}}""".stripMargin
          if (maxRetry <= 0 ) Failure (new RuntimeException(msg))
          else {
            log.warn(s"Trying with a new session factory, $msg")
            sessionFactory = conf.buildSessionFactory(serviceRegistry)
            if (timeout > 0)
              sleep(timeout)
            iterWhileRestarting(5000, maxRetry-1)
          }
        case e: HibernateException =>
          log.error("Database error", e)
          session.getTransaction.rollback()
          Failure(e)
      }
    }
    iterWhileRestarting()
  }
}
