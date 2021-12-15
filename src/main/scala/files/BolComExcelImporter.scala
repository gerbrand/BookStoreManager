package files

import java.io._
import java.net.URL
import java.util.{Calendar, Date}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat, QUOTE_NONNUMERIC}
import org.apache.poi.ss.usermodel.{RichTextString, Row}
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}
import xls.PoiUtils.updateCellIfNeeded

import scala.collection.mutable
import scala.util.Try

/**
 * Reads an Excel-file from Bol.com containing product-offers for shop-in-shop
 */
object BolComExcelImporter {

  /*
  head: List(Reference, EAN, Condition, Stock, Price, Deliverycode, Offer description, For sale, Title)
   */

  case class ProductEntry(reference: String, ean: String, condition: String, stock: Int, price: BigDecimal, deliveryCode: String, description: Option[String], longDescription: String, forSale: Boolean, title: Option[String], images: Option[Array[URL]])

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
    Range(4, aanbod.getLastRowNum + 1 ).to(LazyList).map(aanbod.getRow(_)).filterNot(row => row==null || row.getLastCellNum<7)
  }

  def toEntry(row: XSSFRow) = {
    assert(row!=null && row.getLastCellNum>=7)
    ProductEntry(reference = row.getCell(0).getStringCellValue,
      ean = row.getCell(1).getStringCellValue,
      condition = row.getCell(2).getStringCellValue,
      stock = Option(row.getCell(3).getStringCellValue).map(Integer.parseInt(_)).getOrElse(0),
      price = Option(row.getCell(4).getStringCellValue).map(BigDecimal(_)).getOrElse(0),
      deliveryCode = row.getCell(5).getStringCellValue,
      longDescription = row.getCell(6).getStringCellValue,
      forSale = fromJaNee(row.getCell(7).getStringCellValue),
      title = Option(row.getCell(8).getStringCellValue).filterNot(_.isBlank).filterNot(_.matches("[\\d\\s\"]*")),
      description = Option(row.getCell(10)).map(_.getStringCellValue),
      images = Option(row.getCell(9)).map(_.getStringCellValue).filterNot(_.isBlank).map(_.split(",").map(u => new URL(u)))
    )
  }

  def updateEntryIfNeeded(row: XSSFRow, enrichment: Option[products.BookInformation]) = {
    updateCellIfNeeded(row, 8, enrichment.flatMap(_.title))
    updateCellIfNeeded(row, 9, enrichment.map(_.images).filterNot(_.isEmpty).map(_.mkString(",")))
  }


  def openWorkbook(in: InputStream) = {
    val aanbodWorkbook = new XSSFWorkbook(in)
    aanbodWorkbook.close()
  }
}
