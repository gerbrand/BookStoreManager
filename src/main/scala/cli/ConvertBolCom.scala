package cli

import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.net.URL

import api.{AbeBooks, BolComOpenApi, GoogleBooks, LibraryThing}
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import files.BolCom.ProductEntry
import files.{BolCom, WooCommerce}
import files.WooCommerce.writeWoocommerceCsv
import org.apache.commons.lang3.StringUtils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utils.FutureUtil

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce
 */
object ConvertBolCom {
  val log = LoggerFactory.getLogger(getClass)

  import products.ProductEnricher._

  final def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { case (thread, throwable) => {
      log.error(s"Uncaught exception in thread $thread", throwable)
      System.exit(0)
    }
    }

    implicit val ec = ExecutionContext.global

    if (args.length > 0) {
      val aanbodFile = new File(args(0))
      val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

      val woocommerceFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + ".csv")
      val out = new FileOutputStream(woocommerceFile)
      val writer = WooCommerce.toWriter(out)
      writer.writeRow(WooCommerce.header)


      val entryRows = BolCom.getEntryRows(aanbodWorkbook)

      def close() = {
        Future {
          aanbodWorkbook.close()
          writer.close()
          out.close()
        }
      }

      val result = FutureUtil.sequentialTraverse(entryRows)(row => {
        val entry = BolCom.toEntry(row)
        enrichEntry(entry).map(enrichedEntry => {
          writer.writeRow(WooCommerce.toCsvRow(enrichedEntry))
          BolCom.writeEntry(row, enrichedEntry)
        })
      }).flatMap(_ => close())
      result.recoverWith { case e => {
        log.error("Could not process entire file", e)
        close()
      }
      }

      Await.result(result, 30.minute)
    } else {
      System.err.println("Please specify Bol.com aanbod file as parameter")
    }
  }
}
