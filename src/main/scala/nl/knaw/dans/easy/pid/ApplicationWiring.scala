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
 * Intializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration) extends PidGeneratorComponent with SeedStorageComponent with DebugEnhancedLogging {
  private val firstSeedDoi = configuration.properties.getLong("pid-generator.types.doi.firstSeed")
  private val namespaceDoi = configuration.properties.getString("pid-generator.types.doi.namespace")
  private val dashPositionDoi = configuration.properties.getInt("pid-generator.types.doi.dashPosition")
  private val illegalCharsDoi = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v')
  private val lengthDoi = 7

  private val firstSeedUrn = configuration.properties.getLong("pid-generator.types.urn.firstSeed")
  private val namespaceUrn = configuration.properties.getString("pid-generator.types.urn.namespace")
  private val dashPositionUrn = configuration.properties.getInt("pid-generator.types.urn.dashPosition")
  private val illegalCharsUrn = Map.empty[Char, Char]
  private val lengthUrn = 6

  private val database = new Database
  val databaseAccess = new DatabaseAccess(
    dbDriverClassName = configuration.properties.getString("pid-generator.database.driver-class"),
    dbUrl = configuration.properties.getString("pid-generator.database.url"),
    dbUsername = Option(configuration.properties.getString("pid-generator.database.username")),
    dbPassword = Option(configuration.properties.getString("pid-generator.database.password"))
  )
  databaseAccess.initConnectionPool()

  override val seedStorage: SeedStorage = SeedStorage(Map(
    DOI -> firstSeedDoi,
    URN -> firstSeedUrn
  ))(database, databaseAccess)

  override val pidGenerator: PidGenerator = new PidGenerator(Map(
    DOI -> PidFormatter(namespaceDoi, dashPositionDoi, illegalCharsDoi, lengthDoi),
    URN -> PidFormatter(namespaceUrn, dashPositionUrn, illegalCharsUrn, lengthUrn)
  ))
}
