package domain
import com.google.api.client.json.{GenericJson, Json, JsonParser}
import com.google.api.services.books.model.Volume.VolumeInfo
import org.mongodb.scala.bson.BsonDocument
import products.BookInformation

import java.net.URL
import java.time.ZonedDateTime
import java.util.Date
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
case class ProductInfo(ean: String, title: String, description: Option[String] = None, longDescription: String, images: Option[Seq[String]] = None )

abstract class ExternalProductInfo[T]{
  def ean: String
  def sourceType: String
  def date: Date
  def originalProductInfo: Option[T]

  def bookInformation: Option[BookInformation]
}

case class ExternalProductInfoFromBolCom(ean: String, sourceType: String = "bolcom", date:Date = new Date(), originalProductInfo: Option[com.bol.api.openapi_4_0.Product]) extends ExternalProductInfo[com.bol.api.openapi_4_0.Product] {
  val bookInformation = {
    for {
      productInfo <- originalProductInfo
      images = productInfo.getImages.map(i => new URL(i.getUrl)).toSeq
      title = Option(productInfo.getTitle)
      description =  Option(productInfo.getShortDescription)
    } yield(BookInformation(images = images, title = title, description = description))
  }
}

case class ExternalProductInfoFromGoogleBooks(ean: String, sourceType: String = "googlebooks", date:Date = new Date(), originalProductInfo: Option[BsonDocument], bookInformation: Option[BookInformation]) extends ExternalProductInfo[BsonDocument]
