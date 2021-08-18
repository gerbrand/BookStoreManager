package files

import java.io.OutputStream

import com.github.tototoshi.csv.CSVWriter
import files.BolCom.{ProductEntry, format, fromBoolean}
import products.BookInformation


object WooCommerce {

  case class WooCommerceEntry(
  ID:String, SKU:String, Type:String, Stock:Int, `Regular price`:BigDecimal, `Short description`:Option[String], `Description`:String, `Published`:Boolean, `Name`:Option[String], `images`:Option[Seq[String]], `Shipping class`:String, codition:String
                             )
  /**
   * TODO Use API instead of CSV
   */

  // Based on https://docs.woocommerce.com/document/product-csv-importer-exporter/dummy-data/
  val header = List("ID", "SKU", "Type", "Stock", "Regular price", "Short description", "Description", "Published", "Name", "images", "Shipping class", "Attribute 2 name", "Attribute 2 value(s)")

  def toWriter(out: OutputStream) =  {
    CSVWriter.open(out)(format)
  }

  def toWooCommerceEntry(e: ProductEntry, enrichment: Option[BookInformation]) = {
    WooCommerceEntry(
      e.reference, e.ean, "simple", e.stock, e.price, e.description, e.longDescription, e.forSale, enrichment.map(_.title), enrichment.map(_.images), e.deliveryCode, e.condition)
  }

  def toCsvRow(e: WooCommerceEntry) = {
    List(e.ID, e.SKU, e.Type, e.Stock, e.`Regular price`, e.`Short description`.orElse(e.`Name`).getOrElse(""), e.`Description`, fromBoolean(e.`Published`), e.`Name`.getOrElse(""), e.images.map(_.mkString(",")).getOrElse(""), e.`Shipping class`, "condition", e.codition)
  }

  def writeWoocommerceCsv(bookEntries: Seq[WooCommerceEntry], out: OutputStream) = {
    val writer = CSVWriter.open(out)(format)
    writer.writeRow(header)
    bookEntries.foreach(e => {
      writer.writeRow(toCsvRow(e))
    })
    writer.close()
  }
}
