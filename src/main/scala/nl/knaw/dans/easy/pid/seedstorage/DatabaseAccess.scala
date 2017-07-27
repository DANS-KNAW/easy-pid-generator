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
package nl.knaw.dans.easy.pid.seedstorage

import java.sql.Connection
import javax.sql.DataSource

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.dbcp2.BasicDataSource
import resource.managed

import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

class DatabaseAccess(dbDriverClassName: String,
                     dbUrl: String,
                     dbUsername: Option[String],
                     dbPassword: Option[String]) extends DebugEnhancedLogging {

  require(usernamePasswordCheck,
    "database username and password should either be both defined or not defined")

  private def usernamePasswordCheck: Boolean = {
    (dbUsername, dbPassword) match {
      case (Some(_), Some(_)) | (None, None) => true
      case _ => false
    }
  }

  type ConnectionPool = DataSource with AutoCloseable

  private var pool: ConnectionPool = _

  protected def createConnectionPool: ConnectionPool = {
    new BasicDataSource {
      this.setDriverClassName(dbDriverClassName)
      this.setUrl(dbUrl)
      dbUsername.foreach(this.setUsername)
      dbPassword.foreach(this.setPassword)
    }
  }

  def initConnectionPool(): Try[Unit] = Try {
    logger.info("Creating database connection...")
    pool = createConnectionPool
    logger.info(s"Database connected with URL = $dbUrl, user = $dbUsername, password = ****.")
  }

  def closeConnectionPool(): Try[Unit] = Try {
    logger.info("Closing database connection...")
    pool.close()
    logger.info("Database connection closed.")
  }

  def doTransaction[T](actionFunc: Connection => Try[T]): Try[T] = {
    managed(pool.getConnection)
      .map(connection => {
        connection.setAutoCommit(false)
        val savepoint = connection.setSavepoint()

        actionFunc(connection)
          .doIfSuccess(_ => {
            connection.commit()
            connection.setAutoCommit(true)
          })
          .recoverWith {
            case NonFatal(e) => Try { connection.rollback(savepoint) }.flatMap(_ => Failure(e))
          }
      })
      .tried
      .flatten
  }
}
