package cli

import files.BolCom
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import xls.PoiUtils

import java.io._
import java.math.{MathContext, RoundingMode}
import java.text.NumberFormat
import scala.math.BigDecimal.RoundingMode.RoundingMode

object UpdatePrices {
  val minPrice = BigDecimal(9.95)

  /**
   * Round to more nicely looking number, rounded to 5 cent
   * @param price
   * @return
   */
  def prettyRoundPrice(price:BigDecimal) = {
    val p=(price*100).setScale(0, BigDecimal.RoundingMode.HALF_EVEN)
    val m = p % 5
    if (m>=3)
      (p + (5-m))  / 100
    else
      (p - m) / 100
  }



  def updatePrice(price:BigDecimal) = {
    if (price<minPrice) minPrice else if (price >=10 && price<=25) prettyRoundPrice(price*1.10) else price
  }

  private def updateBolComAanbodsheet(aanbodFile: File, aanbodFileOut: File) = {
    val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

    // Updating all the prices into a new workshet
    val entryRows = BolCom.getEntryRows(aanbodWorkbook)

    entryRows.foreach(row => {
      val bsn = row.getCell(1).getStringCellValue
      val priceCell = row.getCell(4)

      val price = BigDecimal(priceCell.getStringCellValue.replace(',','.'))
      val updatedPrice: BigDecimal = updatePrice(price)

      PoiUtils.updateCellIfNeeded(row, 4, Some(updatedPrice.toString()) )

    })
    val out = new FileOutputStream(aanbodFileOut)

    aanbodWorkbook.write(out)
    out.close()
  }

  def main(args:Array[String]) = {
    val aanbodFile = new File("mijn_huidige_aanbod 2021-08-23.xlsx")
    val aanbodFileOut = new File("mijn_huidige_aanbod 2021-08-23-updated-prices.xlsx")
    updateBolComAanbodsheet(aanbodFile, aanbodFileOut)
  }
}
