package nl.knaw.dans.easy.pid

import org.scalatest.Matchers
import org.scalatest.PropSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import Math.pow
import org.scalacheck.Gen

class PidPackageSpec extends PropSpec with GeneratorDrivenPropertyChecks with Matchers {

  property("Formatted PID starts with configured prefix") {
    forAll(Gen.posNum[Long],
      Gen.alphaStr,
      Gen.choose(2, 36),
      Gen.choose(5, 10)) {
        (pid: Long, prefix: String, radix: Int, len: Int) =>
          val result = format(prefix,
            radix,
            len,
            Map[Char, Char](),
            3)(pid)
          result should startWith(prefix)
      }
  }
  
  // TODO: Test more properties of format 
  
  property("insertDashAt inserts dash at given index") {
    forAll(Gen.alphaStr, Gen.posNum[Int]) { (s: String, i: Int) =>
      whenever(i < s.length) {
        val result = insertDashAt(i)(s)
        result(i) should be('-')
      }
    }
  }
}