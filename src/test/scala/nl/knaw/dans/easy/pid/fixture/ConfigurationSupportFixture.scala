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

import java.io.StringReader

import nl.knaw.dans.easy.pid.Configuration
import org.apache.commons.configuration.PropertiesConfiguration

trait ConfigurationSupportFixture {
  this: TestSupportFixture =>

  val configuration: Configuration = {
    val appProps = new PropertiesConfiguration()
    appProps.load(new StringReader(getApplicationProperties))
    Configuration("1.0.0-UNITTEST", appProps)
  }

  def getApplicationProperties: String = {
    s"""
      |pid-generator.database.driver-class=org.sqlite.JDBC
      |pid-generator.database.url=jdbc:sqlite:$testDir/database.db
      |#pid-generator.database.username=sqlite-doesn't-need-a-username
      |#pid-generator.database.password=sqlite-doesn't-need-a-password
      |
      |pid-generator.daemon.http.port=8060
      |
      |pid-generator.types.urn.namespace=urn:nbn:nl:ui:13-
      |pid-generator.types.urn.dashPosition=4
      |pid-generator.types.urn.firstSeed=1
      |
      |pid-generator.types.doi.namespace=10.5072/dans-
      |pid-generator.types.doi.dashPosition=3
      |pid-generator.types.doi.firstSeed=1073741824
      |
    """.stripMargin
  }
}
