package nl.knaw.dans.easy.pid2.service

import java.nio.file.Paths

import nl.knaw.dans.easy.pid2.generator.{ PidGeneratorApp, PidGeneratorWiring }
import nl.knaw.dans.easy.pid2.server.{ PidServer, PidServlet }
import nl.knaw.dans.easy.pid2.{ Configuration, DatabaseAccess }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.daemon.{ Daemon, DaemonContext }

import scala.util.control.NonFatal

class ServiceStarter extends Daemon with DebugEnhancedLogging {

  var configuration: Configuration = _
  var databaseAccess: DatabaseAccess = _
  var app: PidGeneratorApp = _
  var server: PidServer = _

  override def init(context: DaemonContext): Unit = {
    logger.info("Initializing service ...")

    configuration = Configuration(Paths.get(System.getProperty("app.home")))
    databaseAccess = new DatabaseAccess(
      dbDriverClassName = configuration.properties.getString("pid-generator.database.driver-class"),
      dbUrl = configuration.properties.getString("pid-generator.database.url"),
      dbUsername = Option(configuration.properties.getString("pid-generator.database.username")),
      dbPassword = Option(configuration.properties.getString("pid-generator.database.password"))
    )
    app = new PidGeneratorApp(new PidGeneratorWiring(configuration, databaseAccess))
    server = new PidServer(configuration.properties.getInt("pid-generator.daemon.http.port"), new PidServlet(app))

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
    server.stop()
      .flatMap(_ => databaseAccess.closeConnectionPool())
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
