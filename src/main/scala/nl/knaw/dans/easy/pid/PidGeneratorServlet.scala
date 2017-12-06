/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra._

import scala.util.{ Failure, Success, Try }

class PidGeneratorServlet(app: PidGeneratorApp) extends ScalatraServlet with DebugEnhancedLogging {
  logger.info("PID Generator Servlet running...")

  get("/") {
    contentType = "text/plain"
    Ok("Persistent Identifier Generator running")
  }

  private def respond(calculateResult: PidType => Try[Pid])(pidType: PidType): ActionResult = {
    calculateResult(pidType).map(Created(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case e: SeedNotInitialized => InternalServerError(e.getMessage)
        case e: DuplicatePid => InternalServerError(e.getMessage)
        case e: DatabaseException => InternalServerError(e.getMessage)
        case e => InternalServerError(s"Error while generating the next $pidType: ${ e.getMessage }")
      }
  }

  // POST /create?type={doi|urn}
  post("/create") {
    params.get("type").flatMap(parsePid)
      .map(respond(app.generate))
      .getOrElse(BadRequest("No or unknown Pid type specified, either choose 'doi' or 'urn'"))
  }

  // POST /init?type={doi|urn}&seed={...}
  post("/init") {
    (params.get("type").flatMap(parsePid), params.get("seed").map(s => Try { s.toLong })) match {
      case (Some(pidType), Some(Success(seed))) => app.initialize(pidType, seed)
        .map(_ => Created(s"Pid type $pidType is seeded with $seed"))
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .getOrRecover {
          case e: PidAlreadyInitialized => Conflict(e.getMessage)
          case e: DatabaseException => InternalServerError(e.getMessage)
          case e => InternalServerError(s"Error while seeding $pidType: ${ e.getMessage }")
        }
      case (_, Some(Failure(e))) => BadRequest("The seed is not an integer value")
      case (_, _) => BadRequest("Usage: POST /init?type={doi|urn}&seed={...}")
    }
  }

  private def parsePid(pid: String): Option[PidType] = {
    pid match {
      case "doi" => Some(DOI)
      case "urn" => Some(URN)
      case _ => None
    }
  }
}
