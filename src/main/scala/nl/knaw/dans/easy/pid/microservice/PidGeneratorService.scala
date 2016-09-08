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

import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import com.hazelcast.core.HazelcastInstance
import com.typesafe.config.ConfigFactory
import nl.knaw.dans.easy.pid.{PidGenerator, RanOutOfSeeds}
import org.json4s.DefaultFormats
import org.json4s.ext.UUIDSerializer
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object PidGeneratorService {

  val log = LoggerFactory.getLogger(getClass)

  val home = new File(System.getenv("EASY_PID_GENERATOR_HOME"))
  val conf = ConfigFactory.parseFile(new File(home, "cfg/application.conf"))
  // TODO refactor to the other parsing library
  // TODO refactor to parsing the config to a Settings or Parameters object

  val urns = PidGenerator.urnGenerator(conf, home)
  val dois = PidGenerator.doiGenerator(conf, home)

  val inboxName = conf.getString("inbox-name")
  val pollTimeout = conf.getInt("inbox-poll-timeout") milliseconds

  val running = new AtomicBoolean(true)
  val safeToTerminate = new CountDownLatch(1)

  def stop() = running.compareAndSet(true, false)

  def awaitTermination() = safeToTerminate.await()

  val jsonTransformer = JsonTransformer(DefaultFormats + UUIDSerializer + PidTypeSerializer + ResponseResultSerializer)

  def run(implicit hz: HazelcastInstance) = {
    hz.getQueue[String](inboxName)
      .observe(pollTimeout)(running.get)
      .doOnSubscribe(log.trace(s"listening to queue $inboxName"))
      .doOnError(e => log.error(s"an error occured while listening to $inboxName: ${e.getClass.getSimpleName} - ${e.getMessage}", e))
      .retry
      .flatMap(jsonTransformer.parseJSON[RequestMessage](_).map(executeRequest).toObservable)
      .doOnCompleted {
        log.trace(s"stop listening to queue $inboxName; safe to terminate now...")
        safeToTerminate.countDown()
      }
      .subscribe(response => send(response))
  }

  def executeRequest(request: RequestMessage): Response = {
    val RequestMessage(RequestHead(uuid, responseDS), RequestBody(pidType)) = request

    def respond(result: Try[String]): ResponseResult = {
      // TODO replace with `onError` once this is added to the common library
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
    val json = jsonTransformer.writeJSON(message)

    log.trace(s"sending to $responseDS: ($uuid, $message)")

    ds.put(uuid, json)
  }
}
