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

import nl.knaw.dans.easy.pid.generator.{ PidFormatter, PidGeneratorComponent }
import nl.knaw.dans.easy.pid.seedstorage.{ Database, DatabaseAccess, SeedStorageComponent }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration) extends DebugEnhancedLogging
  with PidGeneratorComponent
  with SeedStorageComponent {

  // TODO: Add validation and clear error messages when config is wrong. Or should this go in Configuration?
  val databaseAccess = new DatabaseAccess(
    dbDriverClassName = configuration.properties.getString("pid-generator.database.driver-class"),
    dbUrl = configuration.properties.getString("pid-generator.database.url"),
    dbUsername = Option(configuration.properties.getString("pid-generator.database.username")),
    dbPassword = Option(configuration.properties.getString("pid-generator.database.password"))
  )
  logger.debug("Initializing database connection...")
  databaseAccess.initConnectionPool()

  logger.debug("Setting up SeedStorage component...")
  override val seedStorage: SeedStorage = SeedStorage(Map(
    DOI -> configuration.properties.getLong("pid-generator.types.doi.firstSeed"),
    URN -> configuration.properties.getLong("pid-generator.types.urn.firstSeed")
  ))(new Database, databaseAccess)

  logger.debug("Setting up PidGenerator component...")
  override val pidGenerator: PidGenerator = new PidGenerator(Map(
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
