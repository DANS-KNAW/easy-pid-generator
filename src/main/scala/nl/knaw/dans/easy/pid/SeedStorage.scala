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
import javax.persistence.Entity

import org.hibernate.HibernateException
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.exception.GenericJDBCException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

sealed trait SeedStorage {
  /**
   * Calculates the next PID seed from the previously stored one and makes
   * sure that it is persisted. Returns a Failure if there is no next PID seed or
   * if the new seed could not be persisted
   */
  def calculateAndPersist(nextPid: Long => Option[Long]): Try[Long]
}

@Entity
class Seed() {
  @BeanProperty
  var pidType: String = null

  @BeanProperty
  var value: Long = Long.MinValue
}
object Seed {
  def create(pidType: String, value: Long) = {
    val seed = new Seed
    seed.pidType = pidType
    seed.value = value

    seed
  }
}

case class RanOutOfSeeds() extends Exception

case class DbBasedSeedStorage(key: String, first: Long, hibernateConfig: File) extends SeedStorage {
  val log = LoggerFactory.getLogger(classOf[DbBasedSeedStorage])
  val conf = new Configuration().configure(hibernateConfig)
  val serviceRegistry = new StandardServiceRegistryBuilder().applySettings(conf.getProperties).build()

  var sessionFactory = conf.buildSessionFactory(serviceRegistry)

  override def calculateAndPersist(nextPid: Long => Option[Long]): Try[Long] = {

    //@tailrec
    def iterWhileRestarting(timeout: Int = 5000, maxRetry: Int = 3): Try[Long] = {
      val session = sessionFactory.getCurrentSession
      session.beginTransaction()
      try {
        session.get(classOf[Seed], key) match {
          case seed: Seed =>
            nextPid(seed.value)
              .map(next => {
                val seed = Seed.create(key, next)
                session.merge(seed)
                session.getTransaction.commit()
                Success(next)
              })
              .getOrElse(Failure(RanOutOfSeeds()))
          case _ =>
            log.warn("NO PREVIOUS PID FOUND. THIS SHOULD ONLY HAPPEN ONCE!! INITIALIZING WITH INITIAL SEED FOR {}", key)
            log.info("Initializing seed with value {}", first)
            val seed = Seed.create(key, first)
            session.save(seed)
            session.getTransaction.commit()
            Success(first)
        }
      }
      catch {
        case e: GenericJDBCException if e.getCause.isInstanceOf[PSQLException] =>
          val msg = s"""Database server connection lost
                       |GenericJDBCException ${e.getMessage}
                       |PSQLException ${e.getCause.getMessage}}""".stripMargin
          if (maxRetry <= 0 ) Failure (new RuntimeException(msg))
          else {
            log.warn(s"Trying with a new session factory, $msg")
            sessionFactory = conf.buildSessionFactory(serviceRegistry)
            Thread.sleep(timeout)
            iterWhileRestarting(5000, maxRetry - 1)
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
