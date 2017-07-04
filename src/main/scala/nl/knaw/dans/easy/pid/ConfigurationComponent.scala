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

import java.nio.file.Path

import org.apache.commons.configuration.PropertiesConfiguration
import resource.managed

import scala.io.Source

/**
 * The GeneralProperties contains the application's version number and properties
 */
trait ConfigurationComponent {

  // singleton component, so access component here
  val configuration: Configuration

  trait Configuration {
    def version: String
    def properties: PropertiesConfiguration
  }

  object Configuration {
    def apply(home: Path): Configuration = new Configuration {
      override val version: String = managed(Source.fromFile(home.resolve("version").toFile)).acquireAndGet(_.mkString)
      override val properties = new PropertiesConfiguration(home.resolve("cfg/application.properties").toFile)
    }
  }
}
