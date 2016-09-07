package nl.knaw.dans.easy.pid.microservice

import java.io.File
import java.util.UUID

import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import nl.knaw.dans.easy.pid.{PidGenerator, RanOutOfSeeds}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object PidGeneratorService {

  val log = LoggerFactory.getLogger(getClass)

  val home = new File(System.getenv("EASY_PID_GENERATOR_HOME"))
  val conf = ConfigFactory.parseFile(new File(home, "cfg/application.conf"))
  // TODO refactor to the other parsing library
  // TODO refactor to parsing the config to a Settings or Parameters object

  val urns = PidGenerator.urnGenerator(conf, home)
  val dois = PidGenerator.doiGenerator(conf, home)

  def run(implicit hz: HazelcastInstance) = {
    hz.getQueue[String]("pid-inbox")
      .observe()
      .map(JSON.parseRequest _ andThen executeRequest)
      .subscribe(response => send(response))
  }

  def executeRequest(request: RequestMessage): Response = {
    val RequestMessage(RequestHead(uuid, responseDS), RequestBody(pidType)) = request

    def respond(result: Try[String]): ResponseResult = {
      result match {
        case Success(pid) => ResponseSuccessResult(pid)
        case Failure(RanOutOfSeeds()) => ResponseFailureResult("No more identifiers")
        case Failure(_) => ResponseFailureResult("Error when retrieving previous seed or saving current seed")
      }
    }

    val result = pidType match {
      case URN => respond(urns.next())
      case DOI => respond(dois.next())
      case unknown => ResponseFailureResult(s"Unknown PID type: $unknown")
    }
    val responseMessage = ResponseMessage(ResponseHead(uuid), ResponseBody(pidType, result))

    (uuid, responseDS, responseMessage)
  }

  def send(response: Response)(implicit hz: HazelcastInstance): Unit = {
    val (uuid, responseDS, message) = response
    val ds = hz.getMap[UUID, String](responseDS)
    val json = JSON.writeResponse(message)
    ds.put(uuid, json)
  }
}
