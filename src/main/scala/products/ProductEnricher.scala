package products

import akka.actor.ActorSystem

import java.io.FileNotFoundException
import api.{AbeBooks, BolComOpenApi, GoogleBooks, LibraryThing}
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import files.BolComExcelImporter.ProductEntry
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ProductEnricher(implicit system: ActorSystem, ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val config = system.settings.config

  val bolComOpenApi = new BolComOpenApi(config.getString("bolcom.api.key"))
  val googleBooksApi = new GoogleBooks(config.getString("google.api.key"))
  val libraryThingApi = new LibraryThing(config.getString("librarything.api.key"))

  def findExtraBookInfoIfNeeded(bookEntry: ProductEntry):Option[BookInformation] = {
    /*if (bookEntry.images.isEmpty && bookEntry.title.isEmpty && bookEntry.stock>0 && bookEntry.forSale) {
        findBookInfoByEANViaGoogleBooks(bookEntry.ean)
          .orElse(findBookInfoByEANViaBolCom(bookEntry.ean))
          .orElse(findBookInfoByEANViaLT(bookEntry.ean))
    } else if (bookEntry.title.isEmpty && bookEntry.stock>0 && bookEntry.forSale) {
      findBookInfoByEANViaLT(bookEntry.ean)
    } else*/ if (bookEntry.images.isEmpty && bookEntry.stock>0 && bookEntry.forSale) {

        findBookInfoByEANViaLT(bookEntry.ean)

    } else {
      Some(BookInformation(title = bookEntry.title, description = bookEntry.description, images = bookEntry.images.map(_.toSeq).getOrElse(Seq.empty)))
    }
  }

  private def fuzzyTitle(reference: String):String = {
      StringUtils.capitalize(s"${reference.replaceAll("\\d","")}")
  }

  private def findBookInfoByEANViaGoogleBooks(ean: String): Option[BookInformation] = {
    googleBooksApi.queryGoogleBooksByIsbn(ean).headOption.map(volume => {
        val title = Option(volume.getVolumeInfo.getTitle)
        val description = Option(volume.getVolumeInfo.getDescription)
        val images = Option(volume.getVolumeInfo.getImageLinks).toSeq.flatMap(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge))
        logger.info(s"Via GoogleBooks: $images, $title")
        BookInformation(images = images.map(new URL(_)), title = title, description = description)
      })
  }

  private def findBookInfoByEANViaBolCom(ean: String): Option[BookInformation] = {
    bolComOpenApi.findProducts(ean).headOption.map(product => {
        import scala.jdk.CollectionConverters._
        val title = Option(product.getTitle)

        val images = product.getMedia.asScala.map(_.getUrl).toList
        logger.info(s"Via Bolcom: $images, $title")
        BookInformation(images = images.map(new URL(_)), title = title, description =  Option(product.getShortDescription))
      }).filterNot(b => b.title.isEmpty && b.images.isEmpty && b.description.isEmpty)
  }

  private def findBookInfoByEANViaLT(ean: String): Option[BookInformation] = {
    val metaData = libraryThingApi.getMetaDataByISBN(ean)
    val images = metaData.view.filterKeys(_.endsWith("image")).values.toSeq
    val title:Option[String] = metaData.view.get("og:title")

    if (! (images.isEmpty || title.isEmpty)) {
      logger.info(s"Via LibraryThing: $images, $title")
      Some(BookInformation(images = images.map(new URL(_)), title = title))
    } else {
      None
    }
  }

  private def findBookInfoByAbeBooks(title: Option[String], ean: String) = {
      AbeBooks.getImageUrl(ean).map(url => {
        logger.info(s"Via AbeBooks: $url")
        BookInformation(images = Seq(url), title = title)
      })
  }
}
