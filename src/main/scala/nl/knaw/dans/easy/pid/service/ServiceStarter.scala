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
package nl.knaw.dans.easy.pid.service

import java.nio.file.Paths

import nl.knaw.dans.easy.pid.seedstorage.DatabaseAccess
import nl.knaw.dans.easy.pid.{ ApplicationWiring, Configuration, PidGeneratorApp }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.daemon.{ Daemon, DaemonContext }

import scala.util.control.NonFatal

class ServiceStarter extends Daemon with DebugEnhancedLogging {
  var app: PidGeneratorApp = _
  var service: PidService = _

  override def init(context: DaemonContext): Unit = {
    logger.info("Initializing service...")
    val configuration = Configuration()
    app = new PidGeneratorApp(new ApplicationWiring(configuration))
    service = new PidService(configuration.properties.getInt("pid-generator.daemon.http.port"), new PidServlet(app))
    logger.info("Service initialized.")
  }

  override def start(): Unit = {
    logger.info("Starting service...")
    service.start()
    logger.info("Service started.")
  }

  override def stop(): Unit = {
    logger.info("Stopping service...")
    service.stop()
  }

  override def destroy(): Unit = {
    service.destroy()
    app.destroy()
    logger.info("Service stopped.")
  }
}
