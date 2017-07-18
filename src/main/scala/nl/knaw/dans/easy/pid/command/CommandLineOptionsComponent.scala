package nl.knaw.dans.easy.pid.command

import nl.knaw.dans.easy.pid.ConfigurationComponent
import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand }

trait CommandLineOptionsComponent {
  this: ConfigurationComponent =>

  val commandLine: CommandLineOptions

  class CommandLineOptions(args: Array[String]) extends ScallopConf(args) {
    appendDefaultToDescription = true
    editBuilder(_.setHelpWidth(110))

    printedName = "easy-pid-generator"
    private val _________ = " " * printedName.length
    private val SUBCOMMAND_SEPARATOR = "---\n"
    version(s"$printedName v${ configuration.version }")

    banner(
      s"""
         |Generate a PID (DOI or URN)
         |
         |Usage:
         |
         |$printedName \\
         |${_________}  | generate --DOI
         |${_________}  | generate --URN
         |${_________}  | run-service
         |
      |Options:
    """.stripMargin)

    val generate = new Subcommand("generate") {
      descr("Generate a specified PID")

      val doi: ScallopOption[Boolean] = opt[Boolean]("DOI", noshort = true, descr = "Generate a DOI", default = None)
      val urn: ScallopOption[Boolean] = opt[Boolean]("URN", noshort = true, descr = "Generate a URN", default = None)

      mutuallyExclusive(doi, urn)
      requireOne(doi, urn)

      footer(SUBCOMMAND_SEPARATOR)
    }
    addSubcommand(generate)

    val runService = new Subcommand("run-service") {
      descr("Starts the EASY Pid Generator as a daemon that services HTTP requests")
      footer(SUBCOMMAND_SEPARATOR)
    }
    addSubcommand(runService)

    footer("")
  }
}
