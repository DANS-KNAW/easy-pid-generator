/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid.microservice

import java.util.UUID

import com.hazelcast.config.Config
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import nl.knaw.dans.easy.pid.generator.{PidGenerator, RanOutOfSeeds}
import nl.knaw.dans.easy.pid.{DOI, Hazelcast, Settings, URN}
import org.json4s.DefaultFormats
import org.json4s.ext.UUIDSerializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, OneInstancePerTest}
import rx.lang.scala.observers.TestSubscriber

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

class PidHazelcastServiceSpec extends FlatSpec with Matchers with OneInstancePerTest with BeforeAndAfter with MockFactory {

  val urnGeneratorMock = mock[PidGenerator]
  val doiGeneratorMock = mock[PidGenerator]

  implicit val hz: HazelcastInstance = {
    val config = new Config
    config.setProperty("hazelcast.logging.type", "none")

    new TestHazelcastInstanceFactory(1).newHazelcastInstance(config)
  }
  implicit val settings = Settings(
    home = null,
    inboxName = "pid-test-inbox",
    inboxPollTimeout = 10 milliseconds,
    port = 8080,
    mode = Hazelcast)

  val service = new PidHazelcastService(
    JsonTransformer(DefaultFormats + UUIDSerializer + PidTypeSerializer + ResponseResultSerializer),
    urnGeneratorMock,
    doiGeneratorMock)

  after {
    // shut down the Hazelcast instance after each test
    // a new instance will be created due to `OneInstancePerTest`
    hz.shutdown()
  }

  "run" should "observe the inbox queue and process only the first item that arrives in there" in {
    val responseDS: ResponseDatastructure = "run-result-map"
    val uuid = UUID.randomUUID()
    val pidType = URN

    val urn: String = "my-generated-urn"
    urnGeneratorMock.next _ expects () returning Success(urn)
    doiGeneratorMock.next _ expects () never()

    val run = service.run()
    val testSubscriber = TestSubscriber[Response]()
    run.take(1).subscribe(testSubscriber)

    val inbox = hz.getQueue[String](settings.inboxName)
    val resultMap = hz.getMap[UUID, String](responseDS)

    val json = s"""{"head":{"requestID":"$uuid","responseDS":"$responseDS"},"body":{"pidType":"${pidType.name}"}}"""
    inbox.put(json)

    testSubscriber.awaitTerminalEvent()
    testSubscriber.assertValueCount(1)
    testSubscriber.assertNoErrors()
    testSubscriber.assertCompleted()

    resultMap should contain key uuid

    val response = resultMap.get(uuid)
    response should contain
      s"""
        |"requestID":"$uuid"
      """.stripMargin
    response should contain
      s"""
        |"result":$urn"
      """.stripMargin

    service.running.get shouldBe true // no interaction with running
    service.safeToTerminate.getCount shouldBe 1 // no interaction with safeToTerminate
  }

  it should "observe the inbox queue and process the requests that arrive until the service is stopped" in {
    val responseDS: ResponseDatastructure = "run-result-map"
    val uuid = UUID.randomUUID()
    val pidType = URN

    val urn: String = "my-generated-urn"
    urnGeneratorMock.next _ expects () returning Success(urn)
    doiGeneratorMock.next _ expects () never()

    val run = service.run()
    val testSubscriber = TestSubscriber[Response]()
    run.subscribe(testSubscriber)

    val inbox = hz.getQueue[String](settings.inboxName)
    val json = s"""{"head":{"requestID":"$uuid","responseDS":"$responseDS"},"body":{"pidType":"${pidType.name}"}}"""
    inbox.put(json)

    while (!inbox.isEmpty) {
      // wait until invalidJson is take out of the inbox
    }

    service.stop() shouldBe true // successful service stopping

    testSubscriber.awaitTerminalEvent()
    testSubscriber.assertValueCount(1)
    testSubscriber.assertNoErrors()
    testSubscriber.assertCompleted()

    val resultMap = hz.getMap[UUID, String](responseDS)
    resultMap should contain key uuid // processed the given item

    service.safeToTerminate.getCount shouldBe 0 // successful onCompleted event
  }

  it should "not process any requests that come in after the service is stopped" in {
    val responseDS: ResponseDatastructure = "run-result-map"
    val uuid = UUID.randomUUID()
    val pidType = URN

    urnGeneratorMock.next _ expects () never()
    doiGeneratorMock.next _ expects () never()

    val run = service.run()
    val testSubscriber = TestSubscriber[Response]()
    run.subscribe(testSubscriber)

    service.stop() shouldBe true // successful service stopping

    testSubscriber.awaitTerminalEvent()

    val inbox = hz.getQueue[String](settings.inboxName)

    // send a message after terminating the service
    val json = s"""{"head":{"requestID":"$uuid","responseDS":"$responseDS"},"body":{"pidType":"${pidType.name}"}}"""
    inbox.put(json)

    testSubscriber.assertNoValues()
    testSubscriber.assertNoErrors()
    testSubscriber.assertCompleted()

    inbox should have size 1
    inbox should contain (json)

    val resultMap = hz.getMap[UUID, String](responseDS)
    resultMap shouldBe empty
  }

  it should "discard invalid json" in {
    val responseDS: ResponseDatastructure = "run-result-map"

    val run = service.run()
    val testSubscriber = TestSubscriber[Response]()
    run.subscribe(testSubscriber)

    val inbox = hz.getQueue[String](settings.inboxName)
    val invalidJson = "this is completely invalid json"
    inbox.put(invalidJson)

    while (!inbox.isEmpty) {
      // wait until invalidJson is take out of the inbox
    }

    service.stop()

    testSubscriber.awaitTerminalEvent()
    testSubscriber.assertNoValues()
    testSubscriber.assertNoErrors()
    testSubscriber.assertCompleted()

    inbox shouldBe empty

    val resultMap = hz.getMap[UUID, String](responseDS)
    resultMap shouldBe empty
  }

  it should "discard partially invalid json" in {
    val responseDS: ResponseDatastructure = "run-result-map"
    val uuid = UUID.randomUUID()
    val pidType = URN

    val run = service.run()
    val testSubscriber = TestSubscriber[Response]()
    run.subscribe(testSubscriber)

    val inbox = hz.getQueue[String](settings.inboxName)
    val invalidJson = s"""{"head":{"requestID":"$uuid","responseDS":"$responseDS"},"body":{"invalidPidTypeKey":"${pidType.name}"}}"""
    inbox.put(invalidJson)

    while (!inbox.isEmpty) {
      // wait until invalidJson is take out of the inbox
    }

    service.stop()

    testSubscriber.awaitTerminalEvent()
    testSubscriber.assertNoValues()
    testSubscriber.assertNoErrors()
    testSubscriber.assertCompleted()

    inbox shouldBe empty

    val resultMap = hz.getMap[UUID, String](responseDS)
    resultMap shouldBe empty
  }

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
    val uuid = UUID.randomUUID()
    val response = (uuid, "send-test-map", ResponseMessage(ResponseHead(uuid), ResponseBody(URN, ResponseSuccessResult("test-result"))))

    service.send(response)

    val resultMap = hz.getMap[UUID, String]("send-test-map")
    resultMap.get(uuid) shouldBe s"""{"head":{"requestID":"$uuid"},"body":{"pidType":"urn","result":{"result":"test-result"}}}"""
  }
}
