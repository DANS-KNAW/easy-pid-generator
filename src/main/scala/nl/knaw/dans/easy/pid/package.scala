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
