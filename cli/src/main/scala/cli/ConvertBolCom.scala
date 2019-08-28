package cli

import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URL

import api.{AbeBooks, BolComOpenApi, LibraryThing}
import files.BolCom.{BookEntry, parseBolComAanbod}
import files.WooCommerce
import files.WooCommerce.writeWoocommerceCsv
import org.apache.commons.lang3.StringUtils
import utils.FutureUtil

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Converts an Excel-file from Bol.com containing product-offers for shop-in-shop to a csv suitable
 * for import in Woocommerce
 */
object ConvertBolCom {
  def enrichEntry(bookEntry: BookEntry)(implicit ec: ExecutionContext) = {
    enrichViaBolCom(bookEntry)
      .flatMap(enrichViaAbeBooks)
      .map(fuzzyEnrich)
  }

  private def fuzzyEnrich(bookEntry: BookEntry) = {
    if (bookEntry.title.isEmpty) {
      bookEntry.copy(title = StringUtils.capitalize(s"${bookEntry.reference.replaceAll("\\d","")}"))
    } else {
      bookEntry
    }
  }
  private def enrichViaBolCom(bookEntry: BookEntry)(implicit ec: ExecutionContext) = {
    if (bookEntry.images.isEmpty || bookEntry.title.isEmpty) {
       BolComOpenApi.findProducts(bookEntry.isbn).map(_.headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = product.getTitle
        val images = product.getMedia.asScala.map(_.getUrl).toList
        println(s"Via Bolcom: $images, $title")
        val result = bookEntry.copy(images = images, title = title)
         result
      })).map(_.getOrElse(bookEntry))
    } else {
      Future.successful(bookEntry)
    }
  }

  private def enrichViaLT(bookEntry: BookEntry) = {
    LibraryThing.getMetaDataByISBN(bookEntry.isbn).map(metaData => {
      val images = metaData.view.filterKeys(_.endsWith("image")).values.toSeq
      val title: String = metaData.view.get("og:title").getOrElse("")
      println(s"Via lt: $images, $title")
      bookEntry.copy(images = images, title = title)
    })
  }

  private def enrichViaAbeBooks(bookEntry: BookEntry)(implicit ec: ExecutionContext) = {
    if (bookEntry.images.isEmpty) {
      AbeBooks.getImageUrl(bookEntry.isbn).map(_.map(url => {
        println(s"Via AbeBooks: $url")
        bookEntry.copy(images = Seq(url))
      })).map(_.getOrElse(bookEntry))
    } else {
      Future.successful(bookEntry)
    }
  }

  final def main(args: Array[String]) = {

    implicit val ec = ExecutionContext.global

    if (args.length > 0) {
      val aanbodFile = new File(args(0))
      val inputStream = new FileInputStream(aanbodFile)
      val bookEntries = parseBolComAanbod(inputStream)
      inputStream.close()

      val woocommerceFile = new File(aanbodFile.getParentFile, aanbodFile.getName.split('.')(0) + ".csv")
      val out = new FileOutputStream(woocommerceFile)
      val writer = WooCommerce.toWriter(out)
      writer.writeRow(WooCommerce.header)

      Await.result(FutureUtil.sequentialTraverse(bookEntries)(enrichEntry(_).map(WooCommerce.toCsvRow).map(writer.writeRow)), 15.minute)

      writer.close()
      out.close()
    } else {
      System.err.println("Please specify Bol.com aanbod file as parameter")
    }
  }
}
