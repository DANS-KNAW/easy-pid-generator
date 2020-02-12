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

import better.files.File
import better.files.File.root
import nl.knaw.dans.easy.pid.PidType.PidType
import nl.knaw.dans.easy.pid.database.DatabaseConfiguration
import nl.knaw.dans.easy.pid.generator.PidFormatter
import org.apache.commons.configuration.PropertiesConfiguration

case class Configuration(version: String,
                         serverPort: Int,
                         databaseConfig: DatabaseConfiguration,
                         formatters: Map[PidType, PidFormatter],
                        )

object Configuration {

  def apply(home: File): Configuration = {
    val cfgPath = Seq(
      root / "etc" / "opt" / "dans.knaw.nl" / "easy-pid-generator",
      home / "cfg")
      .find(_.exists)
      .getOrElse { throw new IllegalStateException("No configuration directory found") }
    val properties = new PropertiesConfiguration() {
      setDelimiterParsingDisabled(true)
      load((cfgPath / "application.properties").toJava)
    }

    new Configuration(
      version = (home / "bin" / "version").contentAsString.stripLineEnd,
      serverPort = properties.getInt("pid-generator.daemon.http.port"),
      databaseConfig = DatabaseConfiguration(
        dbDriverClassName = properties.getString("pid-generator.database.driver-class"),
        dbUrl = properties.getString("pid-generator.database.url"),
        dbUsername = Option(properties.getString("pid-generator.database.username")),
        dbPassword = Option(properties.getString("pid-generator.database.password")),
      ),
      formatters = Map(
        PidType.DOI -> new PidFormatter(
          namespace = properties.getString("pid-generator.types.doi.namespace"),
          dashPosition = properties.getInt("pid-generator.types.doi.dashPosition"),
          illegalChars = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v'),
          length = 7,
        ),
        PidType.URN -> new PidFormatter(
          namespace = properties.getString("pid-generator.types.urn.namespace"),
          dashPosition = properties.getInt("pid-generator.types.urn.dashPosition"),
          illegalChars = Map.empty[Char, Char],
          length = 6,
        )
      ),
    )
  }
}
