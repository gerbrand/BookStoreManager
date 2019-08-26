import org.scalatest.{FunSpec, Matchers}

class ConvertBolComSpec extends FunSpec with Matchers {
  describe("Converting Bol.com offer file") {
    it("should parse sample file") {
      val in = getClass.getResourceAsStream("bol_com_mijn_huidige_aanbod.xlsx")
      val entries = ConvertBolCom.parseBolComAanbod(in)
      in.close()
      entries should have size(13)
    }
  }
}