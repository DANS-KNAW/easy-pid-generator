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
package nl.knaw.dans.easy.pid.rest

import nl.knaw.dans.easy.pid.generator.{PidGenerator, RanOutOfSeeds}
import nl.knaw.dans.easy.pid.microservice.SettingsParser
import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import org.slf4j._

import scala.util.{Failure, Success, Try}

class PidService extends ScalatraServlet with ScalateSupport {
  val log = LoggerFactory.getLogger(getClass)

  implicit val settings = SettingsParser.parse

  val urns = PidGenerator.urnGenerator
  val dois = PidGenerator.doiGenerator

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
      params.get("type")
        .map {
          case "urn" => respond(urns.next())
          case "doi" => respond(dois.next())
          case pidType => BadRequest(s"Unknown PID type $pidType")
        }
        .getOrElse(respond(dois.next()))
    } catch {
      case e: Exception => InternalServerError(s"Error: ${e.getClass}, msg = ${e.getMessage}")
    }
  }
}
