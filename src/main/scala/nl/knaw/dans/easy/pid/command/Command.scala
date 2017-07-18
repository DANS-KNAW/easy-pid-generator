package nl.knaw.dans.easy.pid.command

import nl.knaw.dans.easy.pid.service.ServiceWiring
import nl.knaw.dans.lib.error._

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with CommandLineOptionsComponent with ServiceWiring {

  type FeedBackMessage = String

  val commandLine: CommandLineOptions = new CommandLineOptions(args) {
    verify()
  }

  val result: Try[FeedBackMessage] = for {
    _ <- databaseAccess.initConnectionPool()
    msg <- runCommandLine
    _ <- databaseAccess.closeConnectionPool()
  } yield msg

  result.doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${e.getMessage}") }

  private def runCommandLine: Try[FeedBackMessage] = commandLine.subcommand match {
    case Some(generate @ commandLine.generate) =>
      lazy val doi = generate.doi.toOption.map(_ => databaseAccess.doTransaction { implicit connection => doiGenerator.next() })
      lazy val urn = generate.urn.toOption.map(_ => databaseAccess.doTransaction { implicit connection => urnGenerator.next() })
      lazy val fail = Failure(new IllegalArgumentException(s"Unknown generate type"))

      doi orElse urn getOrElse fail
    case Some(_ @ commandLine.runService) => runAsService()
    case _ => Failure(new IllegalArgumentException(s"Unknown command: ${commandLine.subcommand}"))
  }

  private def runAsService(): Try[FeedBackMessage] = Try {
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        logger.info("Stopping service ...")
        server.stop()
        logger.info("Cleaning up ...")
        server.destroy()
        logger.info("Service stopped.")
      }
    })

    server.start()
    logger.info("Service started ...")
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
