package cli

import akka.actor.ActorSystem

import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.net.URL
import api.{AbeBooks, BolComOpenApi, GoogleBooks, LibraryThing}
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import files.BolCom.ProductEntry
import files.{BolCom, WooCommerce}
import files.WooCommerce.writeWoocommerceCsv
import org.apache.commons.lang3.StringUtils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}
import utils.FutureUtil

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import org.slf4j.LoggerFactory
import products.ProductEnricher

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Try}

/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce
 */
object BolComAanbodFileManager {
  val log = LoggerFactory.getLogger(getClass)


  final def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("BolComAanbodFileManager")
    implicit val ec = system.getDispatcher
    sys.addShutdownHook(system.terminate())

    Thread.setDefaultUncaughtExceptionHandler { case (thread, throwable) => {
      log.error(s"Uncaught exception in thread $thread", throwable)
      System.exit(0)
    }
    }

    val productEnricher = new ProductEnricher()(system, ec)

    if (args.length > 0) {
      val aanbodFile = new File(args(0))
      val tempAanbodFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + "_temp.xlsx")
      val aanbodWorkbook = new XSSFWorkbook(aanbodFile)

      val woocommerceFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + ".csv")
      val out = new FileOutputStream(woocommerceFile)
      val writer = WooCommerce.toWriter(out)
      writer.writeRow(WooCommerce.header)


      val entryRows = BolCom.getEntryRows(aanbodWorkbook)

      def close() = {
        Future {
          // Also write back everything
          val xlsOut = new FileOutputStream(tempAanbodFile)
          aanbodWorkbook.write(xlsOut)
          xlsOut.close()

          aanbodWorkbook.close()
          writer.close()
          out.close()
          aanbodFile.delete()
          tempAanbodFile.renameTo(aanbodFile)

        }
      }

      val result = FutureUtil.sequentialTraverse(entryRows)(row => Future.fromTry {
        for {
          entry <- Try{BolCom.toEntry(row)}.recoverWith{case t => Failure(new RuntimeException(s"Could not read row ${row.getRowNum}", t))}
          extraBookInfo <- productEnricher.findExtraBookInfo(entry)
          _ <- Try{BolCom.updateEntryIfNeeded(row, extraBookInfo)}
          _ <- Try{ writer.writeRow(WooCommerce.toCsvRow(WooCommerce.toWooCommerceEntry(entry, extraBookInfo))) }
        } yield()
      }).flatMap(_ => {
        for {
          _ <- close()
        } yield()
      })
      result.recoverWith { case e => {
        log.error("Could not process entire file", e)
        close()
      }
      }
      Await.result(result, 30.minute)
      System.exit(0)


    } else {
      System.err.println("Please specify Bol.com aanbod file as parameter")
    }
  }
}
