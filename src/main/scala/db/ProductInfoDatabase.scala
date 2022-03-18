package db

import akka.actor.ActorSystem
import api.{BolComOpenApi, GoogleBooks, LibraryThing}
import com.bol.api.openapi_4_0
import com.bol.api.openapi_4_0.{Offer, ParentCategory, ParentCategoryPaths}
import com.fasterxml.jackson.databind.{DeserializationFeature, MapperFeature, ObjectMapper}
import com.google.api.services.books.model.Volume.VolumeInfo
import domain.{ExternalProductInfo, ExternalProductInfoFromBolCom, ExternalProductInfoFromGoogleBooks, ProductInfo}
import files.BolComExcelImporter.ProductEntry
import org.bson.UuidRepresentation
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{ReplaceOptions, UpdateOptions}
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import org.mongojack.JacksonCodecRegistry
import org.slf4j.LoggerFactory
import products.BookInformation

import java.net.URL

class ProductInfoDatabase(implicit system: ActorSystem, implicit val ec:ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017/productInformation"

  private val config = system.settings.config

  val bolComOpenApi = new BolComOpenApi(config.getString("bolcom.api.key"))
  val googleBooksApi = new GoogleBooks(config.getString("google.api.key"))
  val libraryThingApi = new LibraryThing(config.getString("librarything.api.key"))

  // Connect to the database: Must be done only once per application
  val client = MongoClient(mongoUri)

  // IUgnore unknown properties, _id is added by mongodb but normally not added in the Java POJOs
  val objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  val jacksonRegistry = new JacksonCodecRegistry(objectMapper, DEFAULT_CODEC_REGISTRY, UuidRepresentation.STANDARD)
  jacksonRegistry.addCodecForClass(classOf[com.bol.api.openapi_4_0.Product])

  val codecRegistry = fromRegistries(fromProviders(classOf[ExternalProductInfoFromBolCom], classOf[ExternalProductInfoFromGoogleBooks], classOf[BookInformation]), jacksonRegistry, DEFAULT_CODEC_REGISTRY )

  val database: MongoDatabase = client.getDatabase("products").withCodecRegistry(codecRegistry)

  val bolComProductInfoCollection: MongoCollection[ExternalProductInfoFromBolCom] = database.getCollection("externalProductInfoFromBolCom")

  val googleBooksProductInfoCollection: MongoCollection[ExternalProductInfoFromGoogleBooks] = database.getCollection("externalProductInfoFromGoogleBooks")

    // Write Documents: insert or update


//    def findByEan(ean: String): Future[Option[ProductInfo]] = {
//      productInfoCollection.find(equal("ean", ean)).first().toFutureOption()
//    }
//
//    def findAll(): Future[Seq[ProductInfo]] = {
//      productInfoCollection.find().collect().toFuture()
//    }
//
//
//    def upsert(p: ProductInfo): Future[UpdateResult] = {
//      productInfoCollection.replaceOne(filter = equal("ean", p.ean), replacement = p, options = ReplaceOptions().upsert(true)).toFuture()
//    }
//
//  def insert(p: ProductInfo) = {
//    productInfoCollection.insertOne(p).toFuture()
//  }

  /*private def getCachedOrLookup[S, T <: ExternalProductInfo[S]](collection: MongoCollection[T])(constructor: String => Option[S] => T)(ean: String, lookup: String => Future[Option[S]]): Future[T] = {
    for {
      cachedProductInfo <- collection.find(equal("ean", ean)).first().toFutureOption()
      externalProductInfo <- cachedProductInfo.map(Future.successful(_)).getOrElse(lookup(ean).map(p => constructor(ean)(p)) )
      _ <- if (cachedProductInfo.isEmpty) collection.insertOne(externalProductInfo).toFuture() else Future.successful(())
    } yield externalProductInfo
  }

  private def constructBolComProductInfo(ean:String)(productInfo: Option[com.bol.api.openapi_4_0.Product]) = ExternalProductInfoFromBolCom(ean = ean, originalProductInfo = productInfo)

  val getBolComCachedOrLookup = getCachedOrLookup(bolComProductInfoCollection)(constructBolComProductInfo)
*/

  def getBolComCachedOrLookup(ean: String, lookup: String => Future[Option[com.bol.api.openapi_4_0.Product]]): Future[ExternalProductInfoFromBolCom] = {
    for {
      cachedProductInfo <- bolComProductInfoCollection.find(equal("ean", ean)).first().toFutureOption()
      externalProductInfo <- cachedProductInfo.map(Future.successful(_)).getOrElse(lookup(ean).map(p => ExternalProductInfoFromBolCom(ean = ean, originalProductInfo = p) ))
      _ <- if (cachedProductInfo.isEmpty) bolComProductInfoCollection.insertOne(externalProductInfo).toFuture() else Future.successful(())
    } yield externalProductInfo
  }

  /**
   * Generalized function, was originally:
   * <pre>
   *     def getBolComCachedOrLookup(ean: String, lookup: String => Future[Option[com.bol.api.openapi_4_0.Product]]): Future[Option[com.bol.api.openapi_4_0.Product]] = {
    for {
      cachedProductInfo <- bolComProductInfoCollection.find(equal("ean", ean)).first().toFutureOption()
      externalProductInfo <- cachedProductInfo.map(Future.successful(_)).getOrElse(lookup(ean).map(p => ExternalProductInfoFromBolCom(ean = ean, originalProductInfo = p) ))
      _ <- if (cachedProductInfo.isEmpty) upsertBolComInfo(externalProductInfo) else Future.successful(())
    } yield externalProductInfo.originalProductInfo
  }
   </pre>
   */


  def volumeInfoToBookInfo(volumeInfo: VolumeInfo): BookInformation = {
    val title = Option(volumeInfo.getTitle)
    val description = Option(volumeInfo.getDescription)
    val images = Option(volumeInfo.getImageLinks).toSeq.flatMap(imageLinks => Option(imageLinks.getMedium).toSeq ++ Option(imageLinks.getThumbnail) ++ Option(imageLinks.getLarge)++ Option(imageLinks.getExtraLarge))
    logger.info(s"Via GoogleBooks: $images, $title")
    BookInformation(images = images.map(new URL(_)), title = title, description = description)

  }
  def getGoogleBooksCachedOrLookup(ean: String): Future[ExternalProductInfoFromGoogleBooks] = {
    for {
      cachedProductInfo <- googleBooksProductInfoCollection.find(equal("ean", ean)).first().toFutureOption()
      externalProductInfo <- cachedProductInfo.map(Future.successful(_)).getOrElse(Future.successful( {
        val volumeInfo = googleBooksApi.queryGoogleBooksByIsbn(ean).headOption.flatMap(v => Option(v.getVolumeInfo))
        ExternalProductInfoFromGoogleBooks(ean = ean, originalProductInfo =volumeInfo.map(vi => BsonDocument(vi.toPrettyString )), bookInformation = volumeInfo.map(volumeInfoToBookInfo))
      }))
      _ <- if (cachedProductInfo.isEmpty) googleBooksProductInfoCollection.insertOne(externalProductInfo).toFuture() else Future.successful(())
    } yield externalProductInfo
  }

  def findBookInfo(entry: ProductEntry) = {
    getGoogleBooksCachedOrLookup(entry.ean).map(_.bookInformation)
  }
}

object ProductInfoDatabase {
  implicit val ec = ExecutionContext.Implicits.global
  implicit val system: ActorSystem = ActorSystem("ProductInfoDatabase")
  val m= new ProductInfoDatabase()(system,ec)
  import m._

//  def main(args: Array[String]) = {
//    val t=new Thread(() => Thread.sleep(1000))
//    t.start()
//    val bolcomInfo = new com.bol.api.openapi_4_0.Product()
//    bolcomInfo.setTitle("Dit is een test voor bolcom")
//    bolcomInfo.setEAN("12348")
//
//    (for {
//      _ <- upsertBolCom(bolcomInfo)
//      result <- findBolComByEan("12348")//findByEan("12348")
//    } yield result).onComplete(_ match {
//      case Success(result) => {
//        println("Resultaat:\n")
//        result.foreach(p => {
//          println(p.getEAN, p.getTitle)
//        }
//        )
//      }
//      case Failure(e) => e.printStackTrace()
//    })
//
//
//    val product = ProductInfo(ean = "12348", title = "Some test product", longDescription="Just for testing")
//    (for {
//      _ <- upsert(product)
//      result <- findAll() //findByEan("12348")
//    } yield result).onComplete(_ match {
//      case Success(result) => {
//        println("Resultaat:\n")
//        result.foreach(p => {
//          println(p)
//        }
//        )
//      }
//      case Failure(e) => e.printStackTrace()
//    })
//  }
}
