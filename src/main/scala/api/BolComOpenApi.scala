package api

import com.bol.api.openapi_4_0.SearchResults
import com.bol.openapi.QueryDataType.DataType
import com.bol.openapi.QuerySearchField.SearchField
import com.bol.openapi.{OpenApiClient, QuerySearchField}
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import reactivemongo.api.bson.collection.BSONSerializationPack
import reactivemongo.api.bson.{BSONDocument, BSONElement, BSONObjectID, BSONString, BSONValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try
class BolComOpenApi(apiKey: String) {
  // From https://partnerblog.bol.com/je-profiel/?updated=true
  val client = OpenApiClient.withDefaultClient(apiKey)

  def findProducts(isbn: String) = {
    val search = client.searchBuilder().dataType(DataType.PRODUCTS).term(isbn)

     val result = search.search()
    result.getProducts.asScala.toList
  }
}

object BolComOpenApi {
  val api = new BolComOpenApi(System.getenv("BOLCOM_OPEN_API_KEY"))
  final def main(args: Array[String]) = {
    if (args.length > 0) {
      val client = api.client
      val search = client.searchBuilder().dataType(DataType.PRODUCTS).allOffers()
      args.foreach(search.term)
      val result = search.search()

      val products = result.getProducts
      val product = products.get(0)

      val mapper = new ObjectMapper()
      val writer = mapper.writerFor(classOf[com.bol.api.openapi_4_0.Product])

      import reactivemongo.play.json._
      import reactivemongo.api.bson.{
        BSONReader, BSONInteger, BSONNull, BSONString
      }

      BSONValue.pretty()
      val jsonStr = writer.writeValueAsString(product)


      import reactivemongo.play.json._

      println(jsonStr)

      println(products.asScala.map(_.getTitle))
    }
  }
}

