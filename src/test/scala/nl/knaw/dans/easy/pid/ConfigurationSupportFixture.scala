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

import java.nio.file.{ Files, Paths }

import scala.collection.JavaConverters._

trait ConfigurationSupportFixture extends ConfigurationComponent {
  this: TestSupportFixture =>

  override val configuration: Configuration = {
    val versionFile = testDir.resolve("version")
    Files.createFile(versionFile)
    Files.write(versionFile, List("version x.y.z").asJava)

    Files.createDirectory(testDir.resolve("cfg/"))
    Files.copy(
      Paths.get(getClass.getResource("/debug-config/application.properties").toURI),
      testDir.resolve("cfg/application.properties"))

    Configuration(testDir)
  }
}
