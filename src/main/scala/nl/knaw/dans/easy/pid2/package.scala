package nl.knaw.dans.easy

package object pid2 {

  sealed abstract class PidType(val name: String)
  case object DOI extends PidType("doi")
  case object URN extends PidType("urn")

  case class RanOutOfSeeds(pidType: PidType) extends Exception(s"No more ${ pidType.name } seeds available.")
}
