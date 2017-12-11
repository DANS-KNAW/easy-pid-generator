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

import java.nio.file.{ Files, Path }
import java.sql.Connection

import nl.knaw.dans.easy.pid.DatabaseAccessComponent
import org.scalatest.BeforeAndAfterEach
import resource.managed

import scala.io.Source

trait SeedDatabaseFixture extends BeforeAndAfterEach with DatabaseAccessComponent {
  this: TestSupportFixture =>

  implicit var connection: Connection = _

  val databaseFile: Path = testDir.resolve("database.db")

  override val databaseAccess: DatabaseAccess = new DatabaseAccess {
    override val dbDriverClassName = "org.sqlite.JDBC"
    override val dbUrl = s"jdbc:sqlite:${ databaseFile.toString }"
    override val dbUsername = Option.empty[String]
    override val dbPassword = Option.empty[String]

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
    Files.deleteIfExists(databaseFile)
    databaseAccess.initConnectionPool()
  }

  override def afterEach(): Unit = {
    connection.close()
    databaseAccess.closeConnectionPool()
    super.afterEach()
  }
}
