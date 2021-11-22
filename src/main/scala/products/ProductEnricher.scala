package products

import akka.actor.ActorSystem

import java.io.FileNotFoundException
import api.{AbeBooks, BolComOpenApi, GoogleBooks, LibraryThing}
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import files.BolCom.ProductEntry
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ProductEnricher(implicit system: ActorSystem, ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(getClass)

  private val config = system.settings.config

  val bolComOpenApi = new BolComOpenApi(config.getString("bolcom.api.key"))
  val googleBooksApi = new GoogleBooks(config.getString("google.api.key"))
  val libraryThingApi = new LibraryThing(config.getString("librarything.api.key"))

  def findExtraBookInfo(bookEntry: ProductEntry)(implicit ec: ExecutionContext):Try[Option[BookInformation]] = {
    if (bookEntry.images.isEmpty && bookEntry.title.isEmpty) {
      for {
        bookInfo <- findBookInfoByEANViaGoogleBooks(bookEntry.ean)
                    .orElse(findBookInfoByEANViaBolCom(bookEntry.ean))
                    .orElse(findBookInfoByEANViaLT(bookEntry.ean))
          .orElse(findBookInfoByEANAbeBooks(None, bookEntry.ean))
      } yield bookInfo
    } else {
      Success(Some(BookInformation(title = bookEntry.title.getOrElse(""), description = bookEntry.description, images = bookEntry.images.map(_.toSeq).getOrElse(Seq.empty))))
    }
  }

  private def fuzzyTitle(reference: String):String = {
      StringUtils.capitalize(s"${reference.replaceAll("\\d","")}")
  }

  private def findBookInfoByEANViaGoogleBooks(ean: String): Try[Option[BookInformation]] = {
    googleBooksApi.queryGoogleBooksByIsbn(ean).map(_.headOption.map(volume => {
        val title = volume.getVolumeInfo.getTitle
        val description = volume.getVolumeInfo.getDescription
        val images = Option(volume.getVolumeInfo.getImageLinks).toSeq.flatMap(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge))
        log.info(s"Via GoogleBooks: $images, $title")
        BookInformation(images = images, title = title, description = Some(description))
      }))
  }

  private def findBookInfoByEANViaBolCom(ean: String): Try[Option[BookInformation]] = {
    bolComOpenApi.findProducts(ean).map(_.headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = product.getTitle

        val images = product.getMedia.asScala.map(_.getUrl).toList
        log.info(s"Via Bolcom: $images, $title")
        BookInformation(images = images, title = title, description =  Option(product.getShortDescription))
      }))
  }

  private def findBookInfoByEANViaLT(ean: String): Try[Option[BookInformation]] = {
    libraryThingApi.getMetaDataByISBN(ean).map(metaData => {
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

  private def findBookInfoByEANAbeBooks(title: Option[String], ean: String) = {
      AbeBooks.getImageUrl(ean).map(_.map(url => {
        log.info(s"Via AbeBooks: $url")
        BookInformation(images = Seq(url), title = title.getOrElse(""))
      }))
  }
}
