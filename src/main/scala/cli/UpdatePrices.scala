package cli

import files.BolCom
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io._
import java.math.{MathContext, RoundingMode}
import java.text.NumberFormat
import scala.math.BigDecimal.RoundingMode.RoundingMode

object UpdatePrices extends App {
  val aanbodFile = new File("mijn_huidige_aanbod 2021-08-18.xlsx")
  val aanbodFileOut = new File("mijn_huidige_aanbod 2021-08-18-updated-prices.xlsx")

  val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

  // Updating all the prices into a new workshet
  val entryRows = BolCom.getEntryRows(aanbodWorkbook)
  val minPrice = BigDecimal(9.95)
  entryRows.foreach(row => {
    val priceCell = BolCom.getPriceCell(row)
    val price = BigDecimal(priceCell.getRawValue)
    val updatedPrice:BigDecimal =  (if (price<minPrice) minPrice else if (price >=10 && price<=25) (price*1.10) else price).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)

    //priceCell.setCellValue(updatedPrice.doubleValue)
    priceCell.setCellValueImpl(updatedPrice.doubleValue)

  })
  val out = new FileOutputStream(aanbodFileOut)

  aanbodWorkbook.write(out)
  out.close()
}
