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

import nl.knaw.dans.easy.pid.generator._
import nl.knaw.dans.easy.pid.service.{ PidServiceComponent, PidServletComponent, ServletMounterComponent }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.daemon.{ Daemon, DaemonContext }

import scala.util.control.NonFatal

class PidServiceStarter extends Daemon
  with DebugEnhancedLogging
  with PropertiesComponent
  with DatabaseAccessComponent
  with DOIGeneratorWiring
  with URNGeneratorWiring
  with PidServletComponent
  with ServletMounterComponent
  with PidServiceComponent {

  private lazy val home = Paths.get(System.getProperty("app.home"))

  override lazy val properties: GeneralProperties = GeneralProperties(home)
  override lazy val databaseAccess: DatabaseAccess = new DatabaseAccess {
    override val dbDriverClassName: String = properties.properties.getString("pid-generator.database.driver-class")
    override val dbUrl: String = properties.properties.getString("pid-generator.database.url")
    override val dbUsername: Option[String] = Option(properties.properties.getString("pid-generator.database.username"))
    override val dbPassword: Option[String] = Option(properties.properties.getString("pid-generator.database.password"))
  }
  override lazy val urns: URNGenerator = new URNGenerator {}
  override lazy val dois: DOIGenerator = new DOIGenerator {}
  override lazy val pidServlet: PidServlet = new PidServlet {}
  override lazy val mounter: ServletMounter = new ServletMounter {}
  override lazy val service: PidService = new PidService(properties.properties.getInt("pid-generator.daemon.http.port"))

  override def init(context: DaemonContext): Unit = {
    logger.info("Initializing service ...")

    // TODO nothing to do?

    logger.info("Service initialized.")
  }

  override def start(): Unit = {
    logger.info("Starting service ...")
    databaseAccess.initConnectionPool()
      .flatMap(_ => service.start())
      .ifSuccess(_ => logger.info("Service started."))
      .ifFailure {
        case NonFatal(e) => logger.error(s"Service startup failed: ${ e.getMessage }", e)
      }
      .onError(throw _)
  }

  override def stop(): Unit = {
    logger.info("Stopping service ...")
    databaseAccess.closeConnectionPool()
      .flatMap(_ => service.stop())
      .ifSuccess(_ => logger.info("Cleaning up ..."))
      .ifFailure {
        case NonFatal(e) => logger.error(s"Service stop failed: ${ e.getMessage }", e)
      }
      .onError(throw _)
  }

  override def destroy(): Unit = {
    service.destroy()
      .ifSuccess(_ => logger.info("Service stopped."))
      .ifFailure {
        case e => logger.error(s"Service destroy failed: ${ e.getMessage }", e)
      }
      .onError(throw _)
  }
}
