package nl.knaw.dans.easy.pid2.generator

import nl.knaw.dans.easy.pid2.PidType

import scala.util.Try

class PidGeneratorApp(wiring: PidGeneratorWiring) {

  def generate(pidType: PidType): Try[String] = {
    wiring.pidGenerator.generate(pidType)
  }
}
