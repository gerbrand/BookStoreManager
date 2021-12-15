package files

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.{FunSpec, Matchers}

class BolComSpec extends FunSpec with Matchers {
  describe("Converting Bol.com offer file") {
    it("should parse sample file") {
      val in = getClass.getResourceAsStream("/bol_com_mijn_huidige_aanbod.xlsx")
      val aanbodWorkbook = new XSSFWorkbook(in)
      val entries = BolComExcelImporter.getEntries(aanbodWorkbook)
      aanbodWorkbook.close()
      in.close()
      entries should have size(13)
    }
  }
}
