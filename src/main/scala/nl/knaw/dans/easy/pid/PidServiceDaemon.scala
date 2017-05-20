package nl.knaw.dans.easy.pid

import org.apache.commons.daemon.{ Daemon, DaemonContext }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.control.NonFatal

class PidServiceDaemon extends Daemon with DebugEnhancedLogging {

  import PidServiceWiring._

  override def init(context: DaemonContext): Unit = {
    logger.info("Initializing service ...")

    // nothing to do

    logger.info("Service initialized.")
  }

  override def start(): Unit = {
    logger.info("Starting service ...")
    databaseAccess.initConnectionPool()
      .flatMap(_ => server.start())
      .doIfSuccess(_ => logger.info("Service started."))
      .doIfFailure {
        case NonFatal(e) => logger.error(s"Service startup failed: ${ e.getMessage }", e)
      }
      .getOrRecover(throw _)
  }

  override def stop(): Unit = {
    logger.info("Stopping service ...")
    databaseAccess.closeConnectionPool()
      .flatMap(_ => server.stop())
      .doIfSuccess(_ => logger.info("Cleaning up ..."))
      .doIfFailure {
        case NonFatal(e) => logger.error(s"Service stop failed: ${ e.getMessage }", e)
      }
      .getOrRecover(throw _)
  }

  override def destroy(): Unit = {
    server.destroy()
      .doIfSuccess(_ => logger.info("Service stopped."))
      .doIfFailure {
        case e => logger.error(s"Service destroy failed: ${ e.getMessage }", e)
      }
      .getOrRecover(throw _)
  }
}
