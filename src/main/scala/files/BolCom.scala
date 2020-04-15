package files

import java.io._
import java.net.URL
import java.util.{Calendar, Date}

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat, QUOTE_NONNUMERIC}
import org.apache.poi.ss.usermodel.{RichTextString, Row}
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}

import scala.collection.mutable

/**
 * Reads an Excel-file from Bol.com containing product-offers for shop-in-shop
 */
object BolCom {

  /*
  head: List(Reference, EAN, Condition, Stock, Price, Deliverycode, Offer description, For sale, Title)
   */

  case class ProductEntry(reference: String, isbn: String, condition: String, stock: Int, price: BigDecimal, deliveryCode: String, description: String, longDescription: String, forSale: Boolean, title: String, images: Seq[String])

  def fromJaNee(v: String): Boolean = v.toUpperCase() == "JA"

  def fromBoolean(v: Boolean): Int = if (v) 1 else 0

  val format = new DefaultCSVFormat() {
    override val quoting = QUOTE_NONNUMERIC
  }

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
      isbn = row.getCell(1).getRawValue,
      condition = row.getCell(2).getRawValue,
      stock = Integer.parseInt(row.getCell(3).getRawValue),
      price = BigDecimal(row.getCell(4).getRawValue),
      deliveryCode = row.getCell(5).getRawValue,
      longDescription = row.getCell(6).getRawValue,
      forSale = fromJaNee(row.getCell(7).getRawValue),
      title = row.getCell(8).getRawValue,
      images = {
        val _images: Option[Seq[String]] = Option(row.getCell(9)).map(_.getRawValue.split(',').map(_.trim).toSeq)
        _images.getOrElse(Seq.empty[String])
      },
      description = Option(row.getCell(10)).map(_.getRawValue).getOrElse(""))
  }

  def writeEntry(row: XSSFRow, entry: ProductEntry) = {
    updateCellIfNeeded(row, 8, entry.title)
    updateCellIfNeeded(row, 9, entry.images)
  }


  def updateCellIfNeeded[T](row: XSSFRow, cellNum: Int, value: T)  = {
    if (! value.toString.isBlank) {
      val cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
      if (cell.getStringCellValue!=value.toString) {
        value match {
          case v:Seq[String] =>cell.setCellFormula(v.mkString("\"",",",""))
          case v:String =>cell.setCellValue(v)
          case v:Double =>cell.setCellValue(v)
          case v:Calendar =>cell.setCellValue(v)
          case v:RichTextString =>cell.setCellValue(v)
          case v:Date =>cell.setCellValue(v)
        }
      }
    }

  }

  def openWorkbook(in: InputStream) = {
    val aanbodWorkbook = new XSSFWorkbook(in)
    aanbodWorkbook
    aanbodWorkbook.close()
  }
}
