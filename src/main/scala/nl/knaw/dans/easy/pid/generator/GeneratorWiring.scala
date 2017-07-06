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

import nl.knaw.dans.easy.pid.{ ConfigurationComponent, DOI, PidType, URN }

trait GeneratorWiring extends URNGeneratorComponent with DOIGeneratorComponent {
  this: GeneratorWiring.Dependencies =>

  override val urnGenerator: URNGenerator = new URNGenerator {
    override val seedStorage: SeedStorage = new SeedStorage {
      override val pidType: PidType = URN
      override val firstSeed: Long = configuration.properties.getLong("pid-generator.types.urn.firstSeed")
    }
    override val formatter: PidFormatter = new PidFormatter {
      override val length: Int = 6
      override val illegalChars: Map[Char, Char] = Map.empty
      override val namespace: String = configuration.properties.getString("pid-generator.types.urn.namespace")
      override val dashPosition: Int = configuration.properties.getInt("pid-generator.types.urn.dashPosition")
    }
  }

  override val doiGenerator: DOIGenerator = new DOIGenerator {
    override val seedStorage: SeedStorage = new SeedStorage {
      override val pidType: PidType = DOI
      override val firstSeed: Long = configuration.properties.getLong("pid-generator.types.doi.firstSeed")
    }
    override val formatter: PidFormatter = new PidFormatter {
      override val length: Int = 7
      override val illegalChars: Map[Char, Char] = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v')
      override val namespace: String = configuration.properties.getString("pid-generator.types.doi.namespace")
      override val dashPosition: Int = configuration.properties.getInt("pid-generator.types.doi.dashPosition")
    }
  }
}

object GeneratorWiring {
  type Dependencies = DOIGeneratorComponent.Dependencies with URNGeneratorComponent.Dependencies with ConfigurationComponent
}
