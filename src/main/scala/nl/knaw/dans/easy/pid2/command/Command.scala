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
package nl.knaw.dans.easy.pid2.command

import java.nio.file.Paths

import nl.knaw.dans.easy.pid2._
import nl.knaw.dans.easy.pid2.generator.{ PidGeneratorApp, PidGeneratorWiring }
import nl.knaw.dans.easy.pid2.server.{ PidServer, PidServlet }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {

  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val databaseAccess = new DatabaseAccess(
    dbDriverClassName = configuration.properties.getString("pid-generator.database.driver-class"),
    dbUrl = configuration.properties.getString("pid-generator.database.url"),
    dbUsername = Option(configuration.properties.getString("pid-generator.database.username")),
    dbPassword = Option(configuration.properties.getString("pid-generator.database.password"))
  )
  val app = new PidGeneratorApp(new PidGeneratorWiring(configuration, databaseAccess))
  val server = new PidServer(configuration.properties.getInt("pid-generator.daemon.http.port"), new PidServlet(app))

  val result: Try[FeedBackMessage] = for {
    _ <- databaseAccess.initConnectionPool()
    msg <- runCommandLine
    _ <- databaseAccess.closeConnectionPool()
  } yield msg

  result.doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  private def runCommandLine: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(generate @ commandLine.generate) =>
      lazy val doi = generate.doi.toOption.map(_ => app.generate(DOI))
      lazy val urn = generate.urn.toOption.map(_ => app.generate(URN))
      lazy val fail = Failure(new IllegalArgumentException(s"Unknown generate type"))

      doi orElse urn getOrElse fail
    case Some(_ @ commandLine.runService) => runAsService()
    case _ => Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }"))
  }

  private def runAsService(): Try[FeedBackMessage] = Try {
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        logger.info("Stopping service ...")
        server.stop()
        logger.info("Cleaning up ...")
        server.destroy()
        logger.info("Service stopped.")
      }
    })

    server.start()
    logger.info("Service started ...")
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
