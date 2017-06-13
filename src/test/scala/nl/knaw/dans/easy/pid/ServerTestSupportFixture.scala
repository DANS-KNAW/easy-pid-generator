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

import scala.sys.process._

trait ServerTestSupportFixture {

  def call(command: String): String = (command !! ProcessLogger(_ => ())).trim

  def callService(path: String = "pids"): String = call(s"curl http://localhost:8060/$path")

  def postUrn: String = call("curl -X POST http://localhost:8060/pids?type=urn")

  def postDoi: String = call("curl -X POST http://localhost:8060/pids?type=doi")

  def successful: String = "Persistent Identifier Generator running"
}
