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

import nl.knaw.dans.easy.pid.generator.{ DatabaseComponent, PidFormatter, PidManagerComponent }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration) extends DebugEnhancedLogging
  with DatabaseAccessComponent
  with PidManagerComponent
  with DatabaseComponent {

  debug("Setting up DatabaseAccess component...")
  override val databaseAccess = DatabaseAccess(
    driverClassName = configuration.properties.getString("pid-generator.database.driver-class"),
    url = configuration.properties.getString("pid-generator.database.url"),
    username = Option(configuration.properties.getString("pid-generator.database.username")),
    password = Option(configuration.properties.getString("pid-generator.database.password"))
  )

  override val database: Database = new Database {}

//  debug("Setting up SeedStorage component...")
//  override val seedStorage: SeedStorage = SeedStorage(Map(
//    DOI -> configuration.properties.getLong("pid-generator.types.doi.firstSeed"),
//    URN -> configuration.properties.getLong("pid-generator.types.urn.firstSeed")
//  ))

  debug("Setting up PidGenerator component...")
  override val pidGenerator: PidManager = new PidManager(Map(
    DOI -> PidFormatter(
      ns = configuration.properties.getString("pid-generator.types.doi.namespace"),
      dp = configuration.properties.getInt("pid-generator.types.doi.dashPosition"),
      il = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v'),
      len = 7),
    URN -> PidFormatter(
      ns = configuration.properties.getString("pid-generator.types.urn.namespace"),
      dp = configuration.properties.getInt("pid-generator.types.urn.dashPosition"),
      il = Map.empty[Char, Char],
      len = 6)))
}
