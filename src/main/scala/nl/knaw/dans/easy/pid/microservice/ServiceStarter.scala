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
    // TODO stop service
  }

  def destroy(): Unit = {
    // TODO do something?
    log.info("Service pid-generator stopped.")
  }
}
