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

import com.hazelcast.Scala.client._
import com.hazelcast.Scala.serialization
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import nl.knaw.dans.easy.pid.{PidGenerator, Service}
import org.apache.commons.daemon.DaemonContext
import org.json4s.DefaultFormats
import org.json4s.ext.UUIDSerializer
import org.slf4j.LoggerFactory

case class HazelcastService(conf: com.typesafe.config.Config) extends Service {
  val log = LoggerFactory.getLogger(getClass)
  implicit var hz: HazelcastInstance = _
  var service: PidGeneratorService = _

  def init(context: DaemonContext): Unit = {
    log.info("Initializing pid-generator service ...")

    implicit val settings = SettingsParser.parse

    val hzConf = new ClientConfig()
    serialization.Defaults.register(hzConf.getSerializationConfig)
    hz = hzConf.newClient()

    service = new PidGeneratorService(
      JsonTransformer(DefaultFormats + UUIDSerializer + PidTypeSerializer + ResponseResultSerializer),
      PidGenerator.urnGenerator,
      PidGenerator.doiGenerator
    )
  }

  def start(): Unit = {
    log.info("Starting pid-generator service ...")

    service.run()
      .doOnError(e => log.error(s"an error occured in the PID service: ${e.getClass.getSimpleName} - ${e.getMessage}", e))
      .subscribe()
  }

  def stop(): Unit = {
    log.info("Stopping pid-generator service ...")
    service.stop()
  }

  def destroy(): Unit = {
    service.awaitTermination()
    hz.shutdown()
    log.info("Service pid-generator stopped.")
  }
}

