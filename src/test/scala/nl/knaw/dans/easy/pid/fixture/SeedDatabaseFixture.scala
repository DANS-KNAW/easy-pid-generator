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
package nl.knaw.dans.easy.pid.fixture

import java.sql.Connection

import nl.knaw.dans.easy.pid.database.{ DatabaseAccess, DatabaseConfiguration }
import org.scalatest.BeforeAndAfterEach
import resource.managed

import scala.io.Source

trait SeedDatabaseFixture extends BeforeAndAfterEach {
  this: TestSupportFixture =>

  implicit var connection: Connection = _

  private val databaseDir = testDir / "database"

  val databaseAccess: DatabaseAccess = new DatabaseAccess(
    DatabaseConfiguration(
      dbDriverClassName = "org.hsqldb.jdbcDriver",
      dbUrl = s"jdbc:hsqldb:file:$databaseDir/db",
      dbUsername = Option.empty[String],
      dbPassword = Option.empty[String],
    ),
  ) {

    override def createConnectionPool: ConnectionPool = {
      val pool = super.createConnectionPool

      managed(pool.getConnection)
        .flatMap(connection => managed(connection.createStatement))
        .and(managed(Source.fromFile(getClass.getClassLoader.getResource("database/database.sql").toURI)).map(_.mkString))
        .acquireAndGet { case (statement, query) => statement.executeUpdate(query) }

      connection = pool.getConnection

      pool
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    if (databaseDir.exists)
      databaseDir.delete()

    databaseAccess.initConnectionPool()
  }

  override def afterEach(): Unit = {
    managed(connection.createStatement).acquireAndGet(_.execute("SHUTDOWN"))
    connection.close()
    databaseAccess.closeConnectionPool()
    super.afterEach()
  }
}
