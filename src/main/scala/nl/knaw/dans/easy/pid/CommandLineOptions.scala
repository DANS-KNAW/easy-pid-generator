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

import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand, ValueConverter, singleArgConverter }

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

  private implicit val pidParser: ValueConverter[PidType] = singleArgConverter(
    PidType.parse(_).getOrElse(throw new IllegalArgumentException("only 'doi' or 'urn' allowed")),
    {
      case e: IllegalArgumentException => Left(e.getMessage)
    })

  val generate = new Subcommand("generate") {
    descr("Generate a specified PID")
    val pidType: ScallopOption[PidType] = trailArg(name = "pid-type", required = true,
      descr = "The type of PID to be generated, either 'doi' or 'urn'")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(generate)

  val initialize = new Subcommand("initialize") {
    descr("Initialize a specified PID with a seed")
    val pidType: ScallopOption[PidType] = trailArg(name = "pid-type", required = true,
      descr = "The type of PID to be generated, either 'doi' or 'urn'")
    val seed: ScallopOption[Long] = trailArg(name = "seed", required = true,
      descr = "The seed to use for this initialization")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(initialize)

  val runService = new Subcommand("run-service") {
    descr("Starts the EASY Pid Generator as a daemon that services HTTP requests")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(runService)

  footer("")
}
