import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.net.URL

import com.github.tototoshi.csv._
import org.apache.poi.xssf.usermodel.XSSFWorkbook


/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce
 */
object ConvertBolCom {
  /*
  head: List(Reference, EAN, Condition, Stock, Price, Deliverycode, Offer description, For sale, Title)
   */

  case class BookEntry(reference: String, ean: String, condition: String, stock: Int, price: BigDecimal, deliveryCode: String, offerDescription: String, forSale: Boolean, title: String, image: Option[URL] = None)

  def fromJaNee(v: String): Boolean = v.toUpperCase() == "JA"

  def fromBoolean(v: Boolean): Int = if (v) 1 else 0

  val format = new DefaultCSVFormat() {
    override val quoting = QUOTE_NONNUMERIC
  }

  def parseBolComAanbod(in: InputStream): Seq[BookEntry] = {
    val aanbodWorkbook = new XSSFWorkbook(in)
    val aanbod = aanbodWorkbook.getSheetAt(0)
    Range(3, aanbod.getLastRowNum + 1).map(r => {
      val row = aanbod.getRow(r)
      BookEntry(reference = row.getCell(0).getStringCellValue,
        ean = row.getCell(1).getStringCellValue,
        condition = row.getCell(2).getStringCellValue,
        stock = Integer.parseInt(row.getCell(3).getStringCellValue),
        price = BigDecimal(row.getCell(4).getStringCellValue),
        deliveryCode = row.getCell(5).getStringCellValue,
        offerDescription = row.getCell(6).getStringCellValue,
        forSale = fromJaNee(row.getCell(7).getStringCellValue),
        title = row.getCell(8).getStringCellValue)
    })
  }

  def writeWoocommerceCsv(bookEntries: Seq[BookEntry], out: OutputStream) = {
    // Based on https://docs.woocommerce.com/document/product-csv-importer-exporter/dummy-data/
    val header = List("ID", "SKU", "Type", "Stock" ,"Regular price","Short description","Published","Name", "images", "Attribute 1 name","Attribute 1 value(s)" )
    val writer = CSVWriter.open(out)(format)
    writer.writeRow(header)
    bookEntries.foreach(e => {
      writer.writeRow(List(e.reference,e.ean, e.condition,e.stock,e.price,e.offerDescription,fromBoolean(e.forSale),e.title,e.image.getOrElse(""),"deliveryCode", e.deliveryCode))
    })
    writer.close()
  }

  final def main(args: Array[String]) = {
    if (args.length>0) {
      val aanbodFile = new File(args(0))
      val inputStream = new FileInputStream(aanbodFile)
      val bookEntries = parseBolComAanbod(inputStream)
      inputStream.close()
      val woocommerceFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + ".csv")
      val out = new FileOutputStream(woocommerceFile)
      writeWoocommerceCsv(bookEntries, out)
      out.close()
    } else {
      System.err.println("Please specify Bol.com aanbod file as parameter")
    }
  }
}
