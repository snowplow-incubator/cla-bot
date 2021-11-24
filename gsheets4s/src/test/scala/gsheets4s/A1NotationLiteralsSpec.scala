package gsheets4s

import eu.timepit.refined.auto._
import gsheets4s.model._
import org.specs2.mutable.Specification

class A1NotationLiteralsSpec extends Specification {
  "A1 notation literal" in {
    a1"Sheet1!A1:B2" should beEqualTo(SheetNameRangeNotation("Sheet1", Range(ColRowPosition("A", 1), ColRowPosition("B", 2))))
  }
}
