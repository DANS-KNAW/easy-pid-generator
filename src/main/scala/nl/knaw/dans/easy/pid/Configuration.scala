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

import java.nio.file.{ Files, Path, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import resource.managed

import scala.io.Source

case class Configuration(version: String, properties: PropertiesConfiguration)

object Configuration {

  def apply(home: Path): Configuration = {
    val cfgPath = Seq(
      Paths.get(s"/etc/opt/dans.knaw.nl/easy-pid-generator/"),
      home.resolve("cfg"))
      .find(Files.exists(_))
      .getOrElse { throw new IllegalStateException("No configuration directory found") }

    new Configuration(
      version = managed(Source.fromFile(home.resolve("bin/version").toFile)).acquireAndGet(_.mkString),
      properties = new PropertiesConfiguration(cfgPath.resolve("application.properties").toFile)
    )
  }
}
