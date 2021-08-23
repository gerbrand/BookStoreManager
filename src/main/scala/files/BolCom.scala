package files

import java.io._
import java.net.URL
import java.util.{Calendar, Date}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat, QUOTE_NONNUMERIC}
import org.apache.poi.ss.usermodel.{RichTextString, Row}
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}
import xls.PoiUtils.updateCellIfNeeded

import scala.collection.mutable

/**
 * Reads an Excel-file from Bol.com containing product-offers for shop-in-shop
 */
object BolCom {

  /*
  head: List(Reference, EAN, Condition, Stock, Price, Deliverycode, Offer description, For sale, Title)
   */

  case class ProductEntry(reference: String, ean: String, condition: String, stock: Int, price: BigDecimal, deliveryCode: String, description: Option[String], longDescription: String, forSale: Boolean, title: Option[String])

  def fromJaNee(v: String): Boolean = v.toUpperCase() == "JA"

  def fromBoolean(v: Boolean): Int = if (v) 1 else 0

  val format = new DefaultCSVFormat() {
    override val quoting = QUOTE_NONNUMERIC
  }

  /**
   * Gets entries from the BolCom product file, with additional column for images
   */
  def getEntries(aanbodWorkbook: XSSFWorkbook) = {
    getEntryRows(aanbodWorkbook).map(row => {
      (row, toEntry(row))
    })
  }

  def getEntryRows(aanbodWorkbook: XSSFWorkbook): LazyList[XSSFRow] = {
    val aanbod = aanbodWorkbook.getSheetAt(0)
    Range(3, aanbod.getLastRowNum + 1).to(LazyList).map(aanbod.getRow(_)).filterNot(row => row==null || row.getLastCellNum<7)
  }

  def toEntry(row: XSSFRow) = {
    assert(row!=null && row.getLastCellNum>=7)
    ProductEntry(reference = row.getCell(0).getRawValue,
      ean = row.getCell(1).getRawValue,
      condition = row.getCell(2).getRawValue,
      stock = Integer.parseInt(row.getCell(3).getRawValue),
      price = BigDecimal(row.getCell(4).getRawValue),
      deliveryCode = row.getCell(5).getRawValue,
      longDescription = row.getCell(6).getRawValue,
      forSale = fromJaNee(row.getCell(7).getRawValue),
      title = Option(row.getCell(8).getRawValue),
      description = Option(row.getCell(10)).map(_.getRawValue)
    )
  }

  def writeEntry(row: XSSFRow, entry: ProductEntry) = {
    updateCellIfNeeded(row, 8, entry.title)
  }




  def openWorkbook(in: InputStream) = {
    val aanbodWorkbook = new XSSFWorkbook(in)
    aanbodWorkbook.close()
  }
}
