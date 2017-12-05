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

import scala.util.Try

class PidGeneratorServlet(app: PidGeneratorApp) extends ScalatraServlet with DebugEnhancedLogging {
  logger.info("PID Generator Servlet running...")

  get("/") {
    contentType = "text/plain"
    Ok("Persistent Identifier Generator running")
  }

  private def respond(pidType: PidType)(calculateResult: PidType => Try[Pid]): ActionResult = {
    calculateResult(pidType).map(Ok(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case e: DatabaseException => InternalServerError(e.getMessage)
        case e: SeedNotInitialized => InternalServerError(e.getMessage)
        case e: DuplicatePid => InternalServerError(e.getMessage)
        case e => InternalServerError(s"Error when generating the next $pidType: ${ e.getMessage }")
      }
  }

  post("/") {
    params.get("type")
      .map {
        case "doi" => respond(DOI)(app.generate)
        case "urn" => respond(URN)(app.generate)
        case pidType => BadRequest(s"Unknown PID type '$pidType'")
      }
      .getOrElse(BadRequest("No Pid type specified, either choose 'doi' or 'urn'"))
  }
}
