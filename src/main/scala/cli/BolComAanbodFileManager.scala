package cli

import akka.actor.ActorSystem
import com.google.api.client.util.IOUtils
import db.ProductInfoDatabase
import files.{BolComExcelImporter, WooCommerceExporter}
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}
import org.slf4j.LoggerFactory
import org.rogach.scallop._

import java.io.{File, FileOutputStream}
import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce *and* wil download images for each product using various sources.
 */
class BolComAanbodFileManager(implicit system: ActorSystem, implicit val ec:ExecutionContext) {


  val log = LoggerFactory.getLogger(getClass)

  val productInfoDatabase = new ProductInfoDatabase()

  def enrichAanbodFile(aanbodFile: File, aanbodFileOut: File): Future[Unit] = {
    val tempAanbodFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + "_temp.xlsx")
    val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

    val entryRows = BolComExcelImporter.getEntryRows(aanbodWorkbook)

    def processRow(row:XSSFRow) = {
      val entry = BolComExcelImporter.toEntry(row) //.recoverWith{case t => Failure(new RuntimeException(s"Could not read row ${row.getRowNum}", t))
      val extraBookInfo = productInfoDatabase.findBookInfo(entry)
      extraBookInfo.flatMap(e => Future.successful(BolComExcelImporter.updateEntryIfNeeded(row, e)))

    }
    val result = entryRows.foldLeft(Future.successful(())){case (f, row) => f.flatMap(_ => processRow(row))}

    result.onComplete(result => {
      log.debug(s"Saving the result to ${aanbodFile}")
      // Also write back everything
      val xlsOut = new FileOutputStream(tempAanbodFile)
      aanbodWorkbook.write(xlsOut)
      xlsOut.close()

      aanbodWorkbook.close()
      aanbodFile.delete()
      log.debug(s"Overwriting ${aanbodFile} with ${tempAanbodFile}")
      tempAanbodFile.renameTo(aanbodFileOut)

      result match {
        case Success(v) => log.info("Successfully processed everything")
        case Failure(e) => log.error("Could not process entire file", e)
      }

    })
    result
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
}

object BolComAanbodFileManager {
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val bolcom = opt[File](required = false)
    val woocommerce = opt[File](required = false)
    val downloadimages = opt[File](required = false)
    val aanbodFile = trailArg[File]()
    verify()
  }

  implicit val system: ActorSystem = ActorSystem("BolComAanbodFileManager")
  implicit val ec = system.getDispatcher
  val m = new BolComAanbodFileManager()
  import m._

  final def main(args: Array[String]): Unit = {

    sys.addShutdownHook(system.terminate())

    Thread.setDefaultUncaughtExceptionHandler { case (thread, throwable) => {
      log.error(s"Uncaught exception in thread $thread", throwable)
      System.exit(1)
    }
    }

    val conf = new Conf(args)

    conf.aanbodFile.foreach(
      aanbodFile => {
        conf.bolcom.foreach(aanbodFileOut => {
          Await.result(enrichAanbodFile( aanbodFile, aanbodFileOut),30.minutes)
        })
        conf.woocommerce.foreach(wcOut => {
          convertToWoocommerce(aanbodFile, wcOut)
        })
        conf.downloadimages.foreach(imageDir => {
          downloadImages(imageDir, aanbodFile)
        })
      }
    )



  }
}
