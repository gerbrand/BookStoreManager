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

  def enrichEntry(bookEntry: ProductEntry)(implicit ec: ExecutionContext):Future[Option[Enrichment]] = {
//    if (! bookEntry.enriched.getOrElse(false)) {
//      enrichViaBolCom(bookEntry)
//        .flatMap(entry => enrichViaGoogleBooks(entry).recover {
//          case e: GoogleJsonResponseException => {
//            log.error("Could not use Google", e)
//            entry
//          }
//        })
//        .flatMap(enrichViaAbeBooks)
//        .flatMap(entry => enrichViaLT(entry).recover {
//          case e: FileNotFoundException => {
//            log.error("Could not use LibraryThing", e)
//            entry
//          }
//        })
//        .map(fuzzyEnrich)
//      //    Future.successful(bookEntry.copy(images=Seq("blah")))
//    } else {
      Future.successful(None)
//    }
  }

  private def fuzzyTitle(bookEntry: ProductEntry):Option[String] = {
      Some(StringUtils.capitalize(s"${bookEntry.reference.replaceAll("\\d","")}")).filterNot(_.isBlank)
  }

  private def enrichViaGoogleBooks(bookEntry: ProductEntry)(implicit ec: ExecutionContext): Future[Option[Enrichment]] = {
      GoogleBooks.queryGoogleBooks(bookEntry.ean).map(_.headOption.map(volume => {
        val title = volume.getVolumeInfo.getTitle
        val images = Option(volume.getVolumeInfo.getImageLinks).map(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge))
        log.info(s"Via GoogleBooks: $images, $title")
        Enrichment(images = images, title = Some(title))
      }))
  }

  private def enrichViaBolCom(bookEntry: ProductEntry)(implicit ec: ExecutionContext): Future[Option[Enrichment]] = {
      BolComOpenApi.findProducts(bookEntry.ean).map(_.headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = product.getTitle
        val images = product.getMedia.asScala.map(_.getUrl).toList
        log.info(s"Via Bolcom: $images, $title")
        Enrichment(images = Some(images), title = Some(title))
      }))
  }

  private def enrichViaLT(bookEntry: ProductEntry)(implicit ec: ExecutionContext): Future[Option[Enrichment]] = {
    LibraryThing.getMetaDataByISBN(bookEntry.ean).map(metaData => {
      val images = metaData.view.filterKeys(_.endsWith("image")).values.toSeq
      val title:Option[String] = metaData.view.get("og:title")
      if (! (images.isEmpty || title.isEmpty)) {
        log.info(s"Via LibraryThing: $images, $title")
        Some(Enrichment(images = Some(images), title = title))
      } else {
        None
      }
    })
  }

  private def imagesViaAbeBooks(bookEntry: ProductEntry)(implicit ec: ExecutionContext) = {
      AbeBooks.getImageUrl(bookEntry.ean).map(_.map(url => {
        log.info(s"Via AbeBooks: $url")
        Seq(url)
      }))
  }
}
