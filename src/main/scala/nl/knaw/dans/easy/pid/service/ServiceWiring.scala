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
package nl.knaw.dans.easy.pid.service

import java.nio.file.Paths

import nl.knaw.dans.easy.pid.generator._
import nl.knaw.dans.easy.pid.server.ServerWiring
import nl.knaw.dans.easy.pid.{ ConfigurationComponent, DatabaseAccessComponent }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

trait ServiceWiring extends ServerWiring
  with GeneratorWiring
  with PidFormatterComponent
  with SeedStorageComponent
  with DatabaseComponent
  with DatabaseAccessComponent
  with ConfigurationComponent {

  private lazy val home = Paths.get(System.getProperty("app.home"))

  override lazy val configuration: Configuration = Configuration(home)
  override val databaseAccess: DatabaseAccess = new DatabaseAccess {
    override val dbDriverClassName: String = configuration.properties.getString("pid-generator.database.driver-class")
    override val dbUrl: String = configuration.properties.getString("pid-generator.database.url")
    override val dbUsername: Option[String] = Option(configuration.properties.getString("pid-generator.database.username"))
    override val dbPassword: Option[String] = Option(configuration.properties.getString("pid-generator.database.password"))

    private def usernamePasswordCheck: Boolean = {
      (dbUsername, dbPassword) match {
        case (Some(_), Some(_)) | (None, None) => true
        case _ => false
      }
    }

    require(usernamePasswordCheck,
      "database username and password should either be both defined or not defined")
  }
  override val database: Database = new Database {}
}
