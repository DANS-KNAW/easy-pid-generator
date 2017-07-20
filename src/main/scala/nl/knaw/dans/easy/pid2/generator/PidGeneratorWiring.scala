package nl.knaw.dans.easy.pid2.generator

import nl.knaw.dans.easy.pid2._

class PidGeneratorWiring(configuration: Configuration, databaseAccess: DatabaseAccess) extends PidGeneratorComponent with SeedStorageComponent {

  private val database = new Database

  private val firstSeedDoi = configuration.properties.getLong("pid-generator.types.doi.firstSeed")
  private val namespaceDoi = configuration.properties.getString("pid-generator.types.doi.namespace")
  private val dashPositionDoi = configuration.properties.getInt("pid-generator.types.doi.dashPosition")
  private val illegalCharsDoi = Map('0' -> 'z', 'o' -> 'y', '1' -> 'x', 'i' -> 'w', 'l' -> 'v')
  private val lengthDoi = 7

  private val firstSeedUrn = configuration.properties.getLong("pid-generator.types.urn.firstSeed")
  private val namespaceUrn = configuration.properties.getString("pid-generator.types.urn.namespace")
  private val dashPositionUrn = configuration.properties.getInt("pid-generator.types.urn.dashPosition")
  private val illegalCharsUrn = Map.empty[Char, Char]
  private val lengthUrn = 6

  override val seedStorage: SeedStorage = SeedStorage(Map(
    DOI -> firstSeedDoi,
    URN -> firstSeedUrn
  ))(database, databaseAccess)

  override val pidGenerator: PidGenerator = new PidGenerator(Map(
    DOI -> PidFormatter(namespaceDoi, dashPositionDoi, illegalCharsDoi, lengthDoi),
    URN -> PidFormatter(namespaceUrn, dashPositionUrn, illegalCharsUrn, lengthUrn)
  ))
}
