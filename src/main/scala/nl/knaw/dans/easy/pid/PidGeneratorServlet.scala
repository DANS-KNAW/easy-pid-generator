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

  private def respond(result: Try[Pid]): ActionResult = {
    result.map(Ok(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case e: RanOutOfSeeds => NotFound(e.getMessage)
        case _ => InternalServerError("Error when retrieving previous seed or saving current seed")
      }
  }

  post("/") {
    params.get("type")
      .map {
        case "doi" => respond(app.generate(DOI))
        case "urn" => respond(app.generate(URN))
        case pidType => BadRequest(s"Unknown PID type '$pidType'")
      }
      .getOrElse(respond(app.generate(DOI)))
  }
}
