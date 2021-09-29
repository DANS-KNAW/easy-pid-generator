/*
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

import nl.knaw.dans.easy.pid.PidType.PidType
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import org.scalatra._

import scala.util.{ Failure, Success, Try }

class PidGeneratorServlet(app: PidGeneratorApp, applicationVersion: String) extends ScalatraServlet
  with ServletLogger
  with PlainLogFormatter
  with LogResponseBodyOnError
  with DebugEnhancedLogging {
  logger.info("PID Generator Servlet running...")

  get("/") {
    contentType = "text/plain"
    Ok(s"Persistent Identifier Generator running (v$applicationVersion)")
  }

  // GET /{doi|urn}/{...}
  get("/:type/:id") {
    doiExists(params.get("type").flatMap(PidType.parse), params.get("id"))
  }

  // GET /{doi|urn}/{...}/{...}
  get("/:type/:prefix/:suffix") {
    val pid = for {
      prefix <- params.get("prefix")
      suffix <- params.get("suffix")
    } yield s"$prefix/$suffix"

    doiExists(params.get("type").flatMap(PidType.parse), pid)
  }

  private def doiExists(maybePidType: Option[PidType], maybePid: Option[Pid]): ActionResult = {
    (maybePidType, maybePid) match {
      case (Some(pidType), Some(pid)) => app.exists(pidType, pid)
        .map {
          case true => NoContent()
          case false => NotFound(s"$pidType $pid doesn't exist")
        }
        .getOrRecover(
          e => InternalServerError(s"Error while checking the existence of $pidType $pid: ${ e.getMessage }"))
      case (_, _) => BadRequest("Usage: GET /{doi|urn}/{...}")
    }
  }

  // POST /create?type={doi|urn}
  post("/create") {
    params.get("type").flatMap(PidType.parse)
      .map(pidType => app.generate(pidType)
        .map(Created(_))
        .getOrRecover {
          case e: PidNotInitialized => InternalServerError(e.getMessage)
          case e: DuplicatePid => InternalServerError(e.getMessage)
          case e: DatabaseException => InternalServerError(e.getMessage)
          case e => InternalServerError(s"Error while generating the next $pidType: ${ e.getMessage }")
        })
      .getOrElse(BadRequest("No or unknown Pid type specified, either choose 'doi' or 'urn'"))
  }

  // POST /init?type={doi|urn}&seed={...}
  post("/init") {
    (params.get("type").flatMap(PidType.parse), params.get("seed").map(s => Try { s.toLong })) match {
      case (Some(pidType), Some(Success(seed))) => app.initialize(pidType, seed)
        .map(_ => Created(s"Pid type $pidType is seeded with $seed"))
        .getOrRecover {
          case e: PidAlreadyInitialized => Conflict(e.getMessage)
          case e: DatabaseException => InternalServerError(e.getMessage)
          case e => InternalServerError(s"Error while seeding $pidType: ${ e.getMessage }")
        }
      case (_, Some(Failure(_))) => BadRequest("The seed is not an integer value")
      case (_, _) => BadRequest("Usage: POST /init?type={doi|urn}&seed={...}")
    }
  }
}
