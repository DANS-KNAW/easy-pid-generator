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
package nl.knaw.dans.easy.pid.generator

import nl.knaw.dans.easy.pid.PropertiesSupportFixture

class URNGeneratorSpec extends PropertiesSupportFixture with URNGeneratorWiring {

  override val urns: URNGenerator = new URNGenerator {}

  "seedStorage" should "have the correct configured first seed" in {
    urns.seedStorage.firstSeed shouldBe 1L
  }

  "generator" should "have the correct configured namespace" in {
    urns.generator.namespace shouldBe "urn:nbn:nl:ui:13-"
  }

  it should "have the correct configured dashPosition" in {
    urns.generator.dashPosition shouldBe 4
  }
}
