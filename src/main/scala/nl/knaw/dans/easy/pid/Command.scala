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

import java.nio.file.Paths

import nl.knaw.dans.easy.pid.service.{ PidService, PidServlet }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration()
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = new PidGeneratorApp(new ApplicationWiring(configuration))
  val result: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(generate @ commandLine.generate) => generate.pidType() match {
      case "doi" => app.generate(DOI)
      case "urn" => app.generate(URN)
    }
    case Some(_ @ commandLine.runService) => runAsService()
    case _ => Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }"))
  }

  result.doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  app.destroy()

  private def runAsService(): Try[FeedBackMessage] = Try {
    val service = new PidService(configuration.properties.getInt("pid-generator.daemon.http.port"), new PidServlet(app))
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        logger.info("Stopping service ...")
        service.stop()
        logger.info("Cleaning up ...")
        service.destroy()
        logger.info("Service stopped.")
      }
    })

    service.start()
    logger.info("Service started ...")
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
