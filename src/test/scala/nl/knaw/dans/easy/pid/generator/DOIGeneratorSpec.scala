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

class DOIGeneratorSpec extends PropertiesSupportFixture with DOIGeneratorWiring {

  override val dois: DOIGenerator = new DOIGenerator {}

  "seedStorage" should "have the correct configured first seed" in {
    dois.seedStorage.firstSeed shouldBe 1073741824L
  }

  "generator" should "have the correct configured namespace" in {
    dois.generator.namespace shouldBe "10.5072/dans-"
  }

  it should "have the correct configured dashPosition" in {
    dois.generator.dashPosition shouldBe 3
  }
}
