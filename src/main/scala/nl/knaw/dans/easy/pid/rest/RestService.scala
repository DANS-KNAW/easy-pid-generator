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
package nl.knaw.dans.easy.pid.rest

import nl.knaw.dans.easy.pid.{Service, Settings}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

class RestService(implicit settings: Settings) extends Service {
  val log = LoggerFactory.getLogger(getClass)
  log.info("Initializing REST pid-generator service ...")

  val server = new Server(settings.port)
  val context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
  context.addEventListener(new ScalatraListener())
  server.setHandler(context)

  // the actual PidRestService is mounted to the server in ScalatraBootstrap

  override def start() = {
    log.info("Starting REST pid-generator service...")
    server.start()
  }

  override def stop() = {
    log.info("Stopping REST pid-generator service...")
    server.stop()
  }

  override def destroy() = {
    server.destroy()
    log.info("Service REST pid-generator stopped.")
  }
}

