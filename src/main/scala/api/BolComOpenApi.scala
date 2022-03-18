package api

import com.bol.api.openapi_4_0.SearchResults
import com.bol.openapi.QueryDataType.DataType
import com.bol.openapi.QuerySearchField.SearchField
import com.bol.openapi.{OpenApiClient, QuerySearchField}
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper


import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try
class BolComOpenApi(apiKey: String)(implicit ec:ExecutionContext) {
  // From https://partnerblog.bol.com/je-profiel/?updated=true
  val client = OpenApiClient.withDefaultClient(apiKey)

  def getProduct(isbn: String) = {
    Future {
      val search = client.searchBuilder().dataType(DataType.PRODUCTS).term(isbn)

      val result = search.search()
      result.getProducts.asScala.toList.headOption
    }
  }

}

object BolComOpenApi {
  implicit val ec = ExecutionContext.Implicits.global
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


      val jsonStr = writer.writeValueAsString(product)


      println(jsonStr)

      println(products.asScala.map(_.getTitle))
    }
  }
}

