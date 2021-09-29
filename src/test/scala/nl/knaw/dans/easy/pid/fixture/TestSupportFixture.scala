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
package nl.knaw.dans.easy.pid.fixture

import better.files.File
import better.files.File.currentWorkingDirectory
import org.scalatest.{ BeforeAndAfterEach, Inside }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait TestSupportFixture extends AnyFlatSpec with Matchers with Inside with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()

    if (testDir.exists) testDir.delete()
    testDir.createDirectories()
  }

  lazy val testDir: File = currentWorkingDirectory / "target" / "test" / getClass.getSimpleName
}
