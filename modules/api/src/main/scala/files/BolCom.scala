package files

import java.io._
import java.net.URL

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat, QUOTE_NONNUMERIC}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * Reads an Excel-file from Bol.com containing product-offers for shop-in-shop
 */
object BolCom {

  /*
  head: List(Reference, EAN, Condition, Stock, Price, Deliverycode, Offer description, For sale, Title)
   */

  case class BookEntry(reference: String, isbn: String, condition: String, stock: Int, price: BigDecimal, deliveryCode: String, offerDescription: String, forSale: Boolean, title: String, images: Seq[String])

  def fromJaNee(v: String): Boolean = v.toUpperCase() == "JA"

  def fromBoolean(v: Boolean): Int = if (v) 1 else 0

  val format = new DefaultCSVFormat() {
    override val quoting = QUOTE_NONNUMERIC
  }

  def parseBolComAanbod(in: InputStream): Seq[BookEntry] = {
    val aanbodWorkbook = new XSSFWorkbook(in)
    val aanbod = aanbodWorkbook.getSheetAt(0)
    Range(3, aanbod.getLastRowNum + 1).map(aanbod.getRow(_)).filterNot(row => row==null || row.getLastCellNum<7).map(row => {
      BookEntry(reference = row.getCell(0).getStringCellValue,
        isbn = row.getCell(1).getStringCellValue,
        condition = row.getCell(2).getStringCellValue,
        stock = Integer.parseInt(row.getCell(3).getStringCellValue),
        price = BigDecimal(row.getCell(4).getStringCellValue),
        deliveryCode = row.getCell(5).getStringCellValue,
        offerDescription = row.getCell(6).getStringCellValue,
        forSale = fromJaNee(row.getCell(7).getStringCellValue),
        title = row.getCell(8).getStringCellValue,
        images = Seq.empty)
    })
  }
}
