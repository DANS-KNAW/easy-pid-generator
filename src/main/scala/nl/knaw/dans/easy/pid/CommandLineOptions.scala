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

import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand }

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-pid-generator"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  version(s"$printedName v${ configuration.version }")

  banner(
    s"""
       |Generate a PID (DOI or URN)
       |
       |Usage:
       |
       |$printedName generate {doi|urn}
       |$printedName run-service
       |
       |Options:
    """.stripMargin)

  val generate = new Subcommand("generate") {
    descr("Generate a specified PID")
    val pidType: ScallopOption[String] = trailArg(name = "pid-type", required = true)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(generate)

  val runService = new Subcommand("run-service") {
    descr("Starts the EASY Pid Generator as a daemon that services HTTP requests")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(runService)

  footer("")
}
