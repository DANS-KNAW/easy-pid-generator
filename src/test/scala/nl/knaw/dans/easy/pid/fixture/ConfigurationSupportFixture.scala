/*
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

import nl.knaw.dans.easy.pid.database.DatabaseConfiguration
import nl.knaw.dans.easy.pid.generator.PidFormatter
import nl.knaw.dans.easy.pid.{ Configuration, PidType }

trait ConfigurationSupportFixture {
  this: TestSupportFixture =>

  val configuration: Configuration = Configuration(
    version = "1.0.0-UNITTEST",
    serverPort = 8060,
    databaseConfig = DatabaseConfiguration(
      dbDriverClassName = "org.hsqldb.jdbcDriver",
      dbUrl = s"jdbc:hsqldb:file:$testDir/database/db",
    ),
    formatters = Map(
      PidType.DOI -> new PidFormatter(
        namespace = "10.5072/dans-",
        dashPosition = 3,
        illegalChars = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v'),
        length = 7,
      ),
      PidType.URN -> new PidFormatter(
        namespace = "urn:nbn:nl:ui:13-",
        dashPosition = 4,
        illegalChars = Map.empty[Char, Char],
        length = 6,
      )
    )
  )
}
