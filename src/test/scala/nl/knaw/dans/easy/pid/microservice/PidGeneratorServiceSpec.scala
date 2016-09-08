package nl.knaw.dans.easy.pid.microservice

import java.util.UUID

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import nl.knaw.dans.easy.pid.{PidGenerator, RanOutOfSeeds}
import org.json4s.DefaultFormats
import org.json4s.ext.UUIDSerializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.util.{Failure, Success}

class PidGeneratorServiceSpec extends FlatSpec with Matchers with BeforeAndAfter with MockFactory {

  val urnGeneratorMock = mock[PidGenerator]
  val doiGeneratorMock = mock[PidGenerator]

  val service = new PidGeneratorService(
    JsonTransformer(DefaultFormats + UUIDSerializer + PidTypeSerializer + ResponseResultSerializer),
    urnGeneratorMock,
    doiGeneratorMock
  )

  after {
    // shut down all Hazelcast instances after each test
    Hazelcast.shutdownAll()
  }

  // TODO continue testing at run

  "executeRequest" should "handle an URN request by generating a new URN and returning that together with the UUID and response datastructure" in {
    val urn: String = "my-generated-urn"
    urnGeneratorMock.next _ expects () returning Success(urn)
    doiGeneratorMock.next _ expects () never()

    val uuid = UUID.randomUUID()
    val responseDatastructure: ResponseDatastructure = "response-datastructure"
    val request = RequestMessage(RequestHead(uuid, responseDatastructure), RequestBody(URN))
    val (uuidResponse, responseDS, responseMessage) = service.executeRequest(request)

    uuidResponse shouldBe uuid
    responseDS shouldBe responseDatastructure
    responseMessage shouldBe ResponseMessage(ResponseHead(uuid), ResponseBody(URN, ResponseSuccessResult(urn)))
  }

  // TODO continue testing URN/DOI met failure results such as RanOutOfSeeds and others

  it should "handle a DOI request by generating a new DOI and returning that together with the UUID and response datastructure" in {
    val doi: String = "my-generated-doi"
    urnGeneratorMock.next _ expects () never()
    doiGeneratorMock.next _ expects () returning Success(doi)

    val uuid = UUID.randomUUID()
    val responseDatastructure: ResponseDatastructure = "response-datastructure"
    val request = RequestMessage(RequestHead(uuid, responseDatastructure), RequestBody(DOI))
    val (uuidResponse, responseDS, responseMessage) = service.executeRequest(request)

    uuidResponse shouldBe uuid
    responseDS shouldBe responseDatastructure
    responseMessage shouldBe ResponseMessage(ResponseHead(uuid), ResponseBody(DOI, ResponseSuccessResult(doi)))
  }

  it should "return a RanOutOfSeeds message when a corresponding error occurs" in {
    val doi: String = "my-generated-doi"
    urnGeneratorMock.next _ expects () never()
    doiGeneratorMock.next _ expects () returning Failure(RanOutOfSeeds())

    val uuid = UUID.randomUUID()
    val responseDatastructure: ResponseDatastructure = "response-datastructure"
    val request = RequestMessage(RequestHead(uuid, responseDatastructure), RequestBody(DOI))
    val (uuidResponse, responseDS, responseMessage) = service.executeRequest(request)

    uuidResponse shouldBe uuid
    responseDS shouldBe responseDatastructure
    responseMessage shouldBe ResponseMessage(ResponseHead(uuid), ResponseBody(DOI, ResponseFailureResult("No more identifiers")))
  }

  it should "return a general error message when another kind of error occurs" in {
    val doi: String = "my-generated-doi"
    urnGeneratorMock.next _ expects () never()
    doiGeneratorMock.next _ expects () returning Failure(new Exception("this text is ignored"))

    val uuid = UUID.randomUUID()
    val responseDatastructure: ResponseDatastructure = "response-datastructure"
    val request = RequestMessage(RequestHead(uuid, responseDatastructure), RequestBody(DOI))
    val (uuidResponse, responseDS, responseMessage) = service.executeRequest(request)

    uuidResponse shouldBe uuid
    responseDS shouldBe responseDatastructure
    responseMessage shouldBe ResponseMessage(ResponseHead(uuid), ResponseBody(DOI, ResponseFailureResult("Error when retrieving previous seed or saving current seed")))
  }

  "send" should "transform the response into json format and put it in the appropriate map" in {
    val config = new Config
    config.setProperty("hazelcast.logging.type", "none")
    implicit val hz = Hazelcast.newHazelcastInstance(config)

    val uuid = UUID.randomUUID()
    val response = (uuid, "send-test-map", ResponseMessage(ResponseHead(uuid), ResponseBody(URN, ResponseSuccessResult("test-result"))))

    service.send(response)

    val resultMap = hz.getMap[UUID, String]("send-test-map")
    resultMap.get(uuid) shouldBe s"""{"head":{"requestID":"$uuid"},"body":{"pidType":"urn","result":{"result":"test-result"}}}"""
  }
}
