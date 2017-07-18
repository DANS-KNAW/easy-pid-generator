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
package nl.knaw.dans.easy.pid.server

import nl.knaw.dans.easy.pid._
import nl.knaw.dans.easy.pid.generator.{ DOIGeneratorComponent, URNGeneratorComponent }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra._

import scala.util.Try

trait PidServletComponent extends DebugEnhancedLogging {
  this: URNGeneratorComponent with DOIGeneratorComponent with DatabaseAccessComponent =>

  val pidServlet: PidServlet

  trait PidServlet extends ScalatraServlet {

    logger.info("PID Generator Servlet running ...")

    get("/") {
      contentType = "text/plain"
      Ok("Persistent Identifier Generator running")
    }

    post("/*") {
      BadRequest("Cannot create PIDs at this URI")
    }

    private def respond(result: Try[String]): ActionResult = {
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
          case "doi" => respond(databaseAccess.doTransaction { implicit connection => doiGenerator.next() })
          case "urn" => respond(databaseAccess.doTransaction { implicit connection => urnGenerator.next() })
          case pidType => BadRequest(s"Unknown PID type '$pidType'")
        }
        .getOrElse(respond(databaseAccess.doTransaction { implicit connection => doiGenerator.next() }))
    }
  }
}
