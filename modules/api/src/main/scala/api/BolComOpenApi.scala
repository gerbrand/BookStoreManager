package api

import com.bol.api.openapi_4_0.SearchResults
import com.bol.openapi.QueryDataType.DataType
import com.bol.openapi.QuerySearchField.SearchField
import com.bol.openapi.{OpenApiClient, QuerySearchField}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
object BolComOpenApi {
  // From https://partnerblog.bol.com/je-profiel/?updated=true
  val client = OpenApiClient.withDefaultClient(System.getenv("BOLCOM_OPEN_API_KEY"))

  def findProducts(isbn: String)(implicit ec:ExecutionContext) = {
    Future {
      val search = client.searchBuilder().dataType(DataType.PRODUCTS).allOffers().cheapestOffer().term(isbn)

       search.search()
    }.map(result => result.getProducts.asScala.toList)
  }

  final def main(args: Array[String]) = {
    if (args.length > 0) {
      val search = client.searchBuilder().dataType(DataType.PRODUCTS).allOffers()
      args.foreach(search.term)
      val result = search.search()

        val products = result.getProducts.asScala

      println(products.map(_.getTitle))
    }
  }
}
