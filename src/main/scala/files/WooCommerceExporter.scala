package files

import java.io.{File, OutputStream}
import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import files.BolComExcelImporter.{ProductEntry, format, fromBoolean}
import products.BookInformation

import java.net.URL


object WooCommerceExporter {

  case class WooCommerceEntry(
                               ID:Int, SKU:String, Type:String, Stock:Int, `Regular price`:BigDecimal, `Short description`:Option[String], `Description`:String, `Published`:Boolean, `Name`:Option[String], `images`:Option[String], `Shipping class`:String, condition:String
                             )
  /**
   * TODO Use API instead of CSV
   */

  // Based on https://docs.woocommerce.com/document/product-csv-importer-exporter/dummy-data/
  val header = List("SKU", "Type", "Stock", "Regular price", "Short description", "Description", "Published", "Name", "images", "Shipping class", "Attribute 2 name", "Attribute 2 value(s)")

  def toWriter(out: OutputStream) =  {
    CSVWriter.open(out)(format)
  }

  def toWooCommerceEntry(entries: List[ProductEntry]): WooCommerceEntry = {
    val e = entries.sortBy(_.price).last
    val stock = entries.map(_.stock).sum
    WooCommerceEntry(
      (e.reference + e.ean).hashCode, e.ean, "simple", stock, e.price, e.description, e.longDescription, e.forSale, e.title, e.images.flatMap(_.headOption).map(_.toExternalForm) /*e.images.map(_ => toLocalImageUrl(e))*/, e.deliveryCode, e.condition)
  }

  def toValidFileName(e: ProductEntry) = {
    //e.title.orElse(e.description).getOrElse(e.ean).replaceAll("\\s[\\s]*","-").replaceAll("[^\\w-]", "") + ".jpeg"
    e.ean+".jpeg"
  }

  def toLocalImageUrl(e: ProductEntry) = {
    val fileName = toValidFileName(e)
    s"https://www.liberactiva.nl/images/products/${fileName}"
  }

  def toCsvRow(e: WooCommerceEntry) = {
    // Not including ID, is ignored by csv import of woocommerce
    List(e.SKU, e.Type, e.Stock, e.`Regular price`, e.`Short description`.orElse(e.`Name`).getOrElse(""), e.`Description`, fromBoolean(e.`Published`), e.`Name`.getOrElse(""), e.images.getOrElse(""), e.`Shipping class`, "condition", e.condition)
  }

  def writeWoocommerceCsv(bookEntries: Seq[WooCommerceEntry], out: OutputStream) = {
    val writer = CSVWriter.open(out)(format)
    writer.writeRow(header)
    bookEntries.foreach(e => {
      writer.writeRow(toCsvRow(e))
    })
    writer.close()
  }

  def readCsv(wcProductExportFile: File) = {
    val reader = CSVReader.open(wcProductExportFile)(format)

    val entries = reader.allWithHeaders().map(row => {
      WooCommerceEntry(ID = Integer.parseInt(row("ID")), SKU = row("SKU"), Type = row("Type"), Stock = Integer.parseInt(row("Stock")), `Regular price` = BigDecimal(row("price")), `Short description` = row.get("description"), `Description` = row("Description"), `Published` = row("Published")==1, `Name` = row.get("Name"), `images` = row.get("images"), `Shipping class` = row("class"), condition = row("condition"))
    })
    reader.close()
    entries.groupBy(_.ID)
  }
}
