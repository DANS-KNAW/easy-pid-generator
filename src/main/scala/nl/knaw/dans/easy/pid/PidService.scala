package nl.knaw.dans.easy.pid

import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import org.slf4j._
import java.io.File
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.Success
import scala.util.Failure

class PidService extends ScalatraServlet with ScalateSupport {
  val log = LoggerFactory.getLogger(getClass)
  val home = new File(System.getenv("EASY_PID_GENERATOR_HOME"))
  val conf = ConfigFactory.parseFile(new File(home, "cfg/application.conf"))
  val urns = PidGenerator(
    DbBasedSeedStorage(
      "urn",
      conf.getLong("types.urn.firstSeed"),
      new File(home, "cfg/hibernate.conf.xml")),
    conf.getLong("types.urn.firstSeed"),
    format(
      prefix = conf.getString("types.urn.namespace"),
      radix = MAX_RADIX,
      len = 6,
      charMap = Map(),
      dashPos = conf.getInt("types.urn.dashPosition")))
  val doiIllegalCharMap = Map(
    '0' -> 'z',
    'o' -> 'y',
    '1' -> 'x',
    'i' -> 'w',
    'l' -> 'v')
  val dois = PidGenerator(
    DbBasedSeedStorage(
      "doi",
      conf.getLong("types.doi.firstSeed"),
      new File(home, "cfg/hibernate.conf.xml")),
    conf.getLong("types.doi.firstSeed"),
    format(
      prefix = conf.getString("types.doi.namespace"),
      radix = MAX_RADIX - doiIllegalCharMap.size,
      len = 7,
      charMap = doiIllegalCharMap,
      dashPos = conf.getInt("types.doi.dashPosition")))
  log.info("PID Generator Service running ...")
      
  get("/") {
    Ok("Persistent Identifier Generator running")
  }

  post("/*") {
    BadRequest("Cannot create PIDs at this URI")
  }

  post("/") {
    def respond(result: Try[String]) = result match {
      case Success(pid) => Ok(pid)
      case Failure(RanOutOfSeeds()) => NotFound("No more identifiers")
      case Failure(_) => InternalServerError("Error when retrieving previous seed or saving current seed")
    }
    try {
    params.get("type") match {
      case Some(pidType) => pidType match {
        case "urn" => respond(urns.next())
        case "doi" => respond(dois.next())
        case pidType => BadRequest(s"Unknown PID type $pidType")
      }
      case None => respond(dois.next())
    }
    } catch {
      case e: Exception => InternalServerError(s"Error: ${e.getClass}, msg = ${e.getMessage}")
    }
  }
}