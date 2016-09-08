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
import org.apache.commons.daemon.{Daemon, DaemonContext}
import org.slf4j.LoggerFactory

class ServiceStarter extends Daemon {

  val log = LoggerFactory.getLogger(getClass)
  var hz: HazelcastInstance = _

  def init(context: DaemonContext): Unit = {
    log.info("Initializing pid-generator service ...")
  }

  def start(): Unit = {
    log.info("Starting pid-generator service ...")

    val conf = new ClientConfig()
    serialization.Defaults.register(conf.getSerializationConfig)
    hz = conf.newClient()

    PidGeneratorService.run(hz) // can't pass this implicitly since `hz` is a variable
  }

  def stop(): Unit = {
    log.info("Stopping pid-generator service ...")
    PidGeneratorService.stop()
  }

  def destroy(): Unit = {
    PidGeneratorService.awaitTermination()
    hz.shutdown()
    log.info("Service pid-generator stopped.")
  }
}
