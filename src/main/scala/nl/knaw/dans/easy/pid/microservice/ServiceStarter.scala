package nl.knaw.dans.easy.pid.microservice

import org.apache.commons.daemon.{Daemon, DaemonContext}
import org.slf4j.LoggerFactory

class ServiceStarter extends Daemon {

  val log = LoggerFactory.getLogger(getClass)

  def init(context: DaemonContext): Unit = {
    log.info("Initializing pid-generator service ...")
  }

  def start(): Unit = {
    log.info("Starting pid-generator service ...")
    // TODO run service
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
