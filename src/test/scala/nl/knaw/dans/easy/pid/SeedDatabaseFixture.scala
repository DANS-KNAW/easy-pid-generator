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
package nl.knaw.dans.easy.pid

import java.nio.file.Files
import java.sql.Connection

import resource.managed
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatest.BeforeAndAfter

import scala.io.Source

trait SeedDatabaseFixture extends TestSupportFixture
  with BeforeAndAfter
  with DatabaseAccessComponent
  with DebugEnhancedLogging {

  implicit var connection: Connection = _

  override val databaseAccess = new DatabaseAccess {
    val dbDriverClassName: String = "org.sqlite.JDBC"
    val dbUrl: String = s"jdbc:sqlite:${ testDir.resolve("seed.db").toString }"
    val dbUsername: Option[String] = Option.empty[String]
    val dbPassword: Option[String] = Option.empty[String]

    override def createConnectionPool: ConnectionPool = {
      val pool = super.createConnectionPool

      managed(pool.getConnection)
        .flatMap(connection => managed(connection.createStatement))
        .and(managed(Source.fromFile(getClass.getClassLoader.getResource("database/seed.sql").toURI)).map(_.mkString))
        .map { case (statement, query) => statement.executeUpdate(query) }
        .tried

      connection = pool.getConnection

      pool
    }
  }

  before {
    databaseAccess.initConnectionPool()
  }

  after {
    connection.close()
    databaseAccess.closeConnectionPool()
    Files.deleteIfExists(testDir.resolve("seed.db"))
  }
}
