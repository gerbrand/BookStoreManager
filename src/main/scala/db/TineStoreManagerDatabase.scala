package db

import com.bol.api.openapi_4_0
import com.bol.api.openapi_4_0.{Offer, ParentCategory, ParentCategoryPaths}
import com.fasterxml.jackson.databind.{DeserializationFeature, MapperFeature, ObjectMapper}
import domain.ProductInfo
import org.bson.UuidRepresentation
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{ReplaceOptions, UpdateOptions}
import org.mongodb.scala.result.UpdateResult
import org.mongojack.JacksonCodecRegistry

object TineStoreManagerDatabase {
  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017/tine_store_manager"

  implicit val ec = ExecutionContext.Implicits.global

  // Connect to the database: Must be done only once per application
  val client = MongoClient(mongoUri)

  // IUgnore unknown properties, _id is added by mongodb but normally not added in the Java POJOs
  val objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  val jacksonRegistry = new JacksonCodecRegistry(objectMapper, DEFAULT_CODEC_REGISTRY, UuidRepresentation.STANDARD)
  jacksonRegistry.addCodecForClass(classOf[com.bol.api.openapi_4_0.Product])
  val codecRegistry = fromRegistries(fromProviders(classOf[ProductInfo]), jacksonRegistry, DEFAULT_CODEC_REGISTRY )

  val database: MongoDatabase = client.getDatabase("products").withCodecRegistry(codecRegistry)

  val productInfoCollection: MongoCollection[ProductInfo] = database.getCollection("productInfos")

  val bolComProductCollection: MongoCollection[com.bol.api.openapi_4_0.Product] = database.getCollection("bolComProducts")

    // Write Documents: insert or update


    def findByEan(ean: String): Future[Option[ProductInfo]] = {
      productInfoCollection.find(equal("ean", ean)).first().toFutureOption()
    }

    def findAll(): Future[Seq[ProductInfo]] = {
      productInfoCollection.find().collect().toFuture()
    }


    def upsert(p: ProductInfo): Future[UpdateResult] = {
      productInfoCollection.replaceOne(filter = equal("ean", p.ean), replacement = p, options = ReplaceOptions().upsert(true)).toFuture()
    }

  def upsertBolCom(p: com.bol.api.openapi_4_0.Product): Future[UpdateResult] = {
    bolComProductCollection.replaceOne(filter = equal("ean", p.getEAN), replacement = p, options = ReplaceOptions().upsert(true)).toFuture()
  }

  def findBolComByEan(ean: String): Future[Option[com.bol.api.openapi_4_0.Product]] = {
    bolComProductCollection.find(equal("ean", ean)).first().toFutureOption()
  }

  def insert(p: ProductInfo) = {
    productInfoCollection.insertOne(p).toFuture()
  }



  def main(args: Array[String]) = {
    val t=new Thread(() => Thread.sleep(1000))
    t.start()
    val bolcomInfo = new com.bol.api.openapi_4_0.Product()
    bolcomInfo.setTitle("Dit is een test voor bolcom")
    bolcomInfo.setEAN("12348")

        (for {
          _ <- upsertBolCom(bolcomInfo)
          result <- findBolComByEan("12348")//findByEan("12348")
        } yield result).onComplete(_ match {
          case Success(result) => {
            println("Resultaat:\n")
            result.foreach(p => {
              println(p.getEAN, p.getTitle)
            }
              )
          }
          case Failure(e) => e.printStackTrace()
        })


    val product = ProductInfo(ean = "12348", title = "Some test product", longDescription="Just for testing")
    (for {
      _ <- upsert(product)
      result <- findAll() //findByEan("12348")
    } yield result).onComplete(_ match {
      case Success(result) => {
        println("Resultaat:\n")
        result.foreach(p => {
          println(p)
        }
          )
      }
      case Failure(e) => e.printStackTrace()
    })
  }
}
