package products

import java.io.FileNotFoundException

import api.{AbeBooks, BolComOpenApi, GoogleBooks, LibraryThing}
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import files.BolCom.ProductEntry
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future}

object ProductEnricher {
  private val log = LoggerFactory.getLogger(getClass)

  def enrichEntry(bookEntry: ProductEntry)(implicit ec: ExecutionContext) = {
    enrichViaBolCom(bookEntry)
      .flatMap(entry => enrichViaGoogleBooks(entry).recover{
        case e: GoogleJsonResponseException => {
          log.error("Could not use Google", e)
          entry}
      })
      .flatMap(enrichViaAbeBooks)
      .flatMap(entry => enrichViaLT(entry).recover{
        case e: FileNotFoundException => {
          log.error("Could not use LibraryThing", e)
          entry}
      })
      .map(fuzzyEnrich)
    //    Future.successful(bookEntry.copy(images=Seq("blah")))
  }

  private def fuzzyEnrich(bookEntry: ProductEntry) = {
    if (bookEntry.title.isEmpty) {
      bookEntry.copy(title = StringUtils.capitalize(s"${bookEntry.reference.replaceAll("\\d","")}"))
    } else {
      bookEntry
    }
  }

  private def enrichViaGoogleBooks(bookEntry: ProductEntry)(implicit ec: ExecutionContext): Future[ProductEntry] = {
    if (bookEntry.images.isEmpty ||  bookEntry.title.isEmpty) {
      GoogleBooks.queryGoogleBooks(bookEntry.isbn).map(_.headOption.map(volume => {
        val title = volume.getVolumeInfo.getTitle
        val images = Option(volume.getVolumeInfo.getImageLinks).map(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge)).getOrElse(Seq.empty)
        log.info(s"Via GoogleBooks: $images, $title")
        bookEntry.copy(images = images, title = title)
      })).map(_.getOrElse(bookEntry))
    } else {
      Future.successful(bookEntry)
    }
  }

  private def enrichViaBolCom(bookEntry: ProductEntry)(implicit ec: ExecutionContext): Future[ProductEntry] = {
    if (bookEntry.images.isEmpty || bookEntry.title.isEmpty) {
      BolComOpenApi.findProducts(bookEntry.isbn).map(_.headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = product.getTitle
        val images = product.getMedia.asScala.map(_.getUrl).toList
        log.info(s"Via Bolcom: $images, $title")
        val result = bookEntry.copy(images = images, title = title)
        result
      })).map(_.getOrElse(bookEntry))
    } else {
      Future.successful(bookEntry)
    }
  }

  private def enrichViaLT(bookEntry: ProductEntry)(implicit ec: ExecutionContext) = {
    LibraryThing.getMetaDataByISBN(bookEntry.isbn).map(metaData => {
      val images = metaData.view.filterKeys(_.endsWith("image")).values.toSeq
      val title: String = metaData.view.get("og:title").getOrElse("")
      if (! (images.isEmpty || title.isEmpty)) {
        log.info(s"Via LibraryThing: $images, $title")
        bookEntry.copy(images = images, title = title)
      } else {
        bookEntry
      }
    })
  }

  private def enrichViaAbeBooks(bookEntry: ProductEntry)(implicit ec: ExecutionContext) = {
    if (bookEntry.images.isEmpty) {
      AbeBooks.getImageUrl(bookEntry.isbn).map(_.map(url => {
        log.info(s"Via AbeBooks: $url")
        bookEntry.copy(images = Seq(url))
      })).map(_.getOrElse(bookEntry))
    } else {
      Future.successful(bookEntry)
    }
  }
}
