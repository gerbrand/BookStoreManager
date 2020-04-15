package files

import java.io.OutputStream

import com.github.tototoshi.csv.CSVWriter
import files.BolCom.{ProductEntry, format, fromBoolean}


object WooCommerce {

  /**
   * TODO Use API instead of CSV
   */

  // Based on https://docs.woocommerce.com/document/product-csv-importer-exporter/dummy-data/
  val header = List("ID", "SKU", "Type", "Stock", "Regular price", "Short description", "Description", "Published", "Name", "images", "Shipping class", "Attribute 2 name", "Attribute 2 value(s)")

  def toWriter(out: OutputStream) =  {
    CSVWriter.open(out)(format)
  }

  def toCsvRow(e: ProductEntry) = {
    List(e.reference, e.isbn, "simple", e.stock, e.price, e.description, e.longDescription, fromBoolean(e.forSale), e.title, e.images.mkString(","), e.deliveryCode, "condition", e.condition)
  }

  def writeWoocommerceCsv(bookEntries: Seq[ProductEntry], out: OutputStream) = {
    val writer = CSVWriter.open(out)(format)
    writer.writeRow(header)
    bookEntries.foreach(e => {
      writer.writeRow(toCsvRow(e))
    })
    writer.close()
  }
}
