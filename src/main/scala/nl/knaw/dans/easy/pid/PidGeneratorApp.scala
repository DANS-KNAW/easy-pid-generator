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

import java.io.Closeable

import scala.util.Try

/**
 * Exposes the application's API to the application's driver (e.g. Command or ServiceStarter).
 *
 * @param wiring object that configures and wires together the application's components
 */
class PidGeneratorApp(wiring: ApplicationWiring) extends Closeable {

  def this(configuration: Configuration) = this(new ApplicationWiring(configuration))

  def generate(pidType: PidType): Try[Pid] = {
    wiring.databaseAccess.doTransaction(implicit connection => wiring.pidManager.generate(pidType))
  }

  def initialize(pidType: PidType, seed: Seed): Try[Unit] = {
    wiring.databaseAccess.doTransaction(implicit connection => wiring.pidManager.initialize(pidType, seed))
  }

  def init(): Try[Unit] = {
    wiring.databaseAccess.initConnectionPool()
  }

  override def close(): Unit = {
    wiring.databaseAccess.closeConnectionPool().unsafeGetOrThrow
  }
}
