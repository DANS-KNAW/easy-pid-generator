/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid.microservice

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object SettingsParser {

  def parse: Settings = {
    val home = new File(System.getenv("EASY_PID_GENERATOR_HOME"))
    val conf = ConfigFactory.parseFile(new File(home, "cfg/application.conf"))
    val inboxName = conf.getString("inbox-name")
    val pollTimeout = conf.getInt("inbox-poll-timeout") milliseconds

    val generators = Map[PidType, GeneratorSettings](
      DOI -> generatorSettings(DOI, conf),
      URN -> generatorSettings(URN, conf)
    )

    Settings(home, inboxName, pollTimeout, generators)
  }

  private def generatorSettings(pidType: PidType, conf: Config): GeneratorSettings = {
    GeneratorSettings(
      conf.getString(s"types.${pidType.name}.namespace"),
      conf.getInt(s"types.${pidType.name}.dashPosition"),
      conf.getLong(s"types.${pidType.name}.firstSeed")
    )
  }
}
