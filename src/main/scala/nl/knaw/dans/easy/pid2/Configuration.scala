package nl.knaw.dans.easy.pid2

import java.nio.file.{ Files, Path, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import resource.managed

import scala.io.Source

case class Configuration(version: String, properties: PropertiesConfiguration)

object Configuration {

  def apply(home: Path): Configuration = {
    val cfgPath = Seq(
      Paths.get(s"/etc/opt/dans.knaw.nl/easy-pid-generator/"),
      home.resolve("cfg"))
      .find(Files.exists(_))
      .getOrElse { throw new IllegalStateException("No configuration directory found") }

    new Configuration(
      version = managed(Source.fromFile(home.resolve("bin/version").toFile)).acquireAndGet(_.mkString),
      properties = new PropertiesConfiguration(cfgPath.resolve("application.properties").toFile)
    )
  }
}
