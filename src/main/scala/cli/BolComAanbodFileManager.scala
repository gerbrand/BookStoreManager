package cli

import akka.actor.ActorSystem
import com.google.api.client.util.IOUtils
import files.{BolComExcelImporter, WooCommerceExporter}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import products.ProductEnricher
import org.rogach.scallop._

import java.io.{File, FileOutputStream}
import java.net.URL

/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce
 */
object BolComAanbodFileManager {
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val bolcom = opt[File](required = false)
    val woocommerce = opt[File](required = false)
    val downloadimages = opt[File](required = false)
    val aanbodFile = trailArg[File]()
    verify()
  }

  val log = LoggerFactory.getLogger(getClass)

  def enrichAanbodFile(productEnricher: ProductEnricher, aanbodFile: File, aanbodFileOut: File): Unit = {
    val tempAanbodFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + "_temp.xlsx")
    val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

    val entryRows = BolComExcelImporter.getEntryRows(aanbodWorkbook)

    try {
      entryRows.foreach(row => {
        val entry = BolComExcelImporter.toEntry(row) //.recoverWith{case t => Failure(new RuntimeException(s"Could not read row ${row.getRowNum}", t))
        val extraBookInfo = productEnricher.findExtraBookInfoIfNeeded(entry)
        BolComExcelImporter.updateEntryIfNeeded(row, extraBookInfo)
      })
    } catch {
      case e: Throwable => log.error("Could not process entire file", e)
    } finally {
      log.debug(s"Saving the result to ${aanbodFile}")
      // Also write back everything
      val xlsOut = new FileOutputStream(tempAanbodFile)
      aanbodWorkbook.write(xlsOut)
      xlsOut.close()

      aanbodWorkbook.close()
      aanbodFile.delete()
      log.debug(s"Overwriting ${aanbodFile} with ${tempAanbodFile}")
      tempAanbodFile.renameTo(aanbodFileOut)
    }
  }

  def convertToWoocommerce(aanbodFile: File, woocommerceFile: File): Unit = {

    val out = new FileOutputStream(woocommerceFile)
    val writer = WooCommerceExporter.toWriter(out)
    writer.writeRow(WooCommerceExporter.header)
    val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

    val entryRows = BolComExcelImporter.getEntryRows(aanbodWorkbook)
      .map(BolComExcelImporter.toEntry)
      .filter(e => e.forSale && e.stock>0)
      .groupBy(e => e.ean)
      .map{case (_, products) => WooCommerceExporter.toWooCommerceEntry( products.toList)}
      .map(updatePriceForWoocommerce)
      .filterNot(e => e.`images`.isEmpty && e.`Name`.isEmpty)

    entryRows.foreach(entry => {
      writer.writeRow(WooCommerceExporter.toCsvRow(entry))
    })
    writer.close()
    out.close()
  }

  def updatePriceForWoocommerce(entry: WooCommerceExporter.WooCommerceEntry): WooCommerceExporter.WooCommerceEntry = {
    entry.copy(`Regular price` = updatePriceForWoocommerce(entry.`Regular price`))
  }

  def updatePriceForWoocommerce(originalPrice: BigDecimal): BigDecimal = {
      val newPrice = originalPrice - originalPrice * 0.15 - 1.99
      if (newPrice>0) newPrice else {
        log.warn("Price {} is already very low", originalPrice)
        originalPrice
      }
  }

  def downloadImages(imageDir: File, aanbodFile: File) = {
    val aanbodWorkbook = new XSSFWorkbook(aanbodFile)
    val entryRows = BolComExcelImporter.getEntryRows(aanbodWorkbook)
      .map(BolComExcelImporter.toEntry)
    imageDir.mkdirs()
    entryRows.foreach(e => {
      e.images.foreach(images => {
        images.headOption.foreach{
          url => {
            val fileName = WooCommerceExporter.toValidFileName(e)
            val outFile = new File(imageDir, WooCommerceExporter.toValidFileName(e))
            if (! outFile.exists()) {
              log.debug("Downloading image {}", url)
              val connection = url.openConnection()
              val contentType = connection.getContentType
              val extension = contentType.split("/")(1)
              if (extension!="jpeg") {
                log.warn("Alternative content type for file {}: {}", fileName, extension)
              }
              val out = new FileOutputStream(outFile)
              IOUtils.copy(connection.getInputStream, out)
              connection.getInputStream.close()
              out.close()
            }
          }
        }
      })
    })
  }

  final def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("BolComAanbodFileManager")
    implicit val ec = system.getDispatcher
    sys.addShutdownHook(system.terminate())

    Thread.setDefaultUncaughtExceptionHandler { case (thread, throwable) => {
      log.error(s"Uncaught exception in thread $thread", throwable)
      System.exit(0)
    }
    }

    val conf = new Conf(args)

    conf.aanbodFile.foreach(
      aanbodFile => {
        conf.bolcom.foreach(aanbodFileOut => {
          val productEnricher = new ProductEnricher()(system, ec)
          enrichAanbodFile(productEnricher, aanbodFile, aanbodFileOut)
        })
        conf.woocommerce.foreach(wcOut => {
          convertToWoocommerce(aanbodFile, wcOut)
        })
        conf.downloadimages.foreach(imageDir => {
          downloadImages(imageDir, aanbodFile)
        })
      }
    )

      System.exit(0)

  }
}
