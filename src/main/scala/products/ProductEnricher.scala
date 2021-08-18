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

  def findBookInfo(ean: String, reference: String, title: Option[String])(implicit ec: ExecutionContext): Future[BookInformation] = {
    for {
      googleInfo <- findBookInfoByEANViaGoogleBooks(ean)
    } yield(googleInfo.getOrElse(BookInformation(title = fuzzyTitle(reference))))
  }
  def enrichEntry(bookEntry: ProductEntry)(implicit ec: ExecutionContext):Future[Option[BookInformation]] = {
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

  private def fuzzyTitle(reference: String):String = {
      StringUtils.capitalize(s"${reference.replaceAll("\\d","")}")
  }

  private def findBookInfoByEANViaGoogleBooks(ean: String)(implicit ec: ExecutionContext): Future[Option[BookInformation]] = {
      GoogleBooks.queryGoogleBooks(ean).map(_.headOption.map(volume => {
        val title = volume.getVolumeInfo.getTitle
        val description = volume.getVolumeInfo.getDescription
        val images = Option(volume.getVolumeInfo.getImageLinks).toSeq.flatMap(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge))
        log.info(s"Via GoogleBooks: $images, $title")
        BookInformation(images = images, title = title, description = Some(description))
      }))
  }

  private def findBookInfoByEANViaBolCom(ean: String)(implicit ec: ExecutionContext): Future[Option[BookInformation]] = {
      BolComOpenApi.findProducts(ean).map(_.headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = product.getTitle

        val images = product.getMedia.asScala.map(_.getUrl).toList
        log.info(s"Via Bolcom: $images, $title")
        BookInformation(images = images, title = title, description =  Option(product.getShortDescription))
      }))
  }

  private def findBookInfoByEANViaLT(ean: String)(implicit ec: ExecutionContext): Future[Option[BookInformation]] = {
    LibraryThing.getMetaDataByISBN(ean).map(metaData => {
      val images = metaData.view.filterKeys(_.endsWith("image")).values.toSeq
      val title:Option[String] = metaData.view.get("og:title")
      if (! (images.isEmpty || title.isEmpty)) {
        log.info(s"Via LibraryThing: $images, $title")
        Some(BookInformation(images = images, title = title.getOrElse("")))
      } else {
        None
      }
    })
  }

  private def indBookInfoByEANAbeBooks(title: Option[String], ean: String)(implicit ec: ExecutionContext) = {
      AbeBooks.getImageUrl(ean).map(_.map(url => {
        log.info(s"Via AbeBooks: $url")
        BookInformation(images = Seq(url), title = title.getOrElse(""))
      }))
  }
}
