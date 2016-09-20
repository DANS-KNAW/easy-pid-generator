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
package nl.knaw.dans.easy.pid

import nl.knaw.dans.easy.pid.microservice.HazelcastService
import nl.knaw.dans.easy.pid.rest.RestService
import org.apache.commons.daemon.{Daemon, DaemonContext}
import org.slf4j.LoggerFactory

class ServiceStarter extends Daemon with SettingsParser {
  val log = LoggerFactory.getLogger(getClass)
  var service: Service = _

  override def init(ctx: DaemonContext): Unit = {
    log.info("Initializing service ...")

    implicit val settings = getSettings
    service = settings.mode match {
      case Rest => new RestService
      case Hazelcast => new HazelcastService
      case unknown => throw new IllegalArgumentException(s"Invalid mode: $unknown. Valid modes are 'rest', 'hazelcast'")
    }
  }

  override def start(): Unit = {
    log.info("Starting service ...")
    service.start()
  }

  override def stop(): Unit = {
    log.info("Stopping service ...")
    service.stop()
  }

  override def destroy(): Unit = {
    service.destroy()
    log.info("Service stopped.")
  }
}
