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
package nl.knaw.dans.easy

import java.io.File

import scala.concurrent.duration.Duration

package object pid {

  // a PID can either be an URN or a DOI
  sealed abstract class PidType(val name: String)
  case object URN extends PidType("urn")
  case object DOI extends PidType("doi")

  case class GeneratorSettings(namespace: String, dashPosition: Int, firstSeed: Long)
  case class Settings(home: File,
                      generatorSettings: Map[PidType, GeneratorSettings] = Map.empty,
                      inboxName: String,
                      inboxPollTimeout: Duration,
                      port: Int,
                      mode: Mode)

  sealed abstract class Mode
  case object Rest extends Mode
  case object Hazelcast extends Mode
}
