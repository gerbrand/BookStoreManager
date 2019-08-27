package files

import java.io.OutputStream

import com.github.tototoshi.csv.CSVWriter
import files.BolCom.{BookEntry, format, fromBoolean}

object WooCommerce {

  // Based on https://docs.woocommerce.com/document/product-csv-importer-exporter/dummy-data/
  val header = List("ID", "SKU", "Type", "Stock", "Regular price", "Short description", "Published", "Name", "images", "Shipping class", "Attribute 2 name", "Attribute 2 value(s)")

  def toWriter(out: OutputStream) =  {
    CSVWriter.open(out)(format)
  }

  def toCsvRow(e: BookEntry) = {
    List(e.reference, e.isbn, "simple", e.stock, e.price, e.offerDescription, fromBoolean(e.forSale), e.title, e.images.mkString(","), e.deliveryCode, "condition", e.condition)
  }

  def writeWoocommerceCsv(bookEntries: Seq[BookEntry], out: OutputStream) = {
    val writer = CSVWriter.open(out)(format)
    writer.writeRow(header)
    bookEntries.foreach(e => {
      writer.writeRow(toCsvRow(e))
    })
    writer.close()
  }
}
