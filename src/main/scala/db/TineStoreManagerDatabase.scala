package db

import com.bol.api.openapi_4_0
import com.bol.api.openapi_4_0.{Offer, ParentCategory, ParentCategoryPaths}
import domain.ProductInfo
import reactivemongo.api.bson.BSONValue.pretty
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}
import reactivemongo.api.bson.{BSONArray, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONInteger, BSONString, Macros, document}

import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

object TineStoreManagerDatabase {
  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017/tine_store_manager"

  implicit val ec = ExecutionContext.Implicits.global

  // Connect to the database: Must be done only once per application
  val driver = AsyncDriver()
  val parsedUri = MongoConnection.fromString(mongoUri)

  implicit def bolcomTracklistWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.TrackList] = new BSONDocumentWriter[com.bol.api.openapi_4_0.TrackList] {
    override def writeTry(ts: openapi_4_0.TrackList): Try[BSONDocument] = {
      Success(document(
        "diskNumber" -> ts.getDiscNumber,
        "track" -> BSONArray(ts.getTrack.asScala.map(t => document("title" -> t.getTitle,"trackNumber"-> t.getTrackNumber)))
      ))
    }
  }
  implicit def bolcomOfferWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.Offer] = new BSONDocumentWriter[com.bol.api.openapi_4_0.Offer] {
    override def writeTry(p: Offer): Try[BSONDocument] = {
      Success(document(
        "id" -> p.getId(),
        "freeshipping" -> p.isFreeshipping.booleanValue(),
        "condition" -> p.getCondition,
        "price" -> p.getPrice.toDouble,
        "listPrice" -> p.getListPrice.toDouble,
        "availabilityCode" -> p.getAvailabilityCode(),
        "availabilityDescription" -> p.getAvailabilityDescription(),
        "comment" -> p.getComment(),
        "seller" -> {
          document("emailAddress" -> p.getSeller.getEmailAddress,
          "displayName" -> p.getSeller.getDisplayName)
          // TODO other fields if needed
        },
        "bestOffer" -> p.isBestOffer.booleanValue(),
        "releaseDate" -> p.getReleaseDate,
      ))
    }
  }

  implicit def bolcomPromotionWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.Promotion] = new BSONDocumentWriter[com.bol.api.openapi_4_0.Promotion] {
    override def writeTry(p: com.bol.api.openapi_4_0.Promotion): Try[BSONDocument] = {
      Success(document(
        "title" -> p.getTitle,
        "url" -> p.getUrl,
        "urlText" -> p.getUrlText,
        "description" -> p.getDescription,
      ))
    }
  }

  implicit def bolcomParentCategoryWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.ParentCategory] = new BSONDocumentWriter[com.bol.api.openapi_4_0.ParentCategory] {
    override def writeTry(p: ParentCategory): Try[BSONDocument] = {
      Success(document(
        "id" -> p.getId,
        "name" -> p.getName,
      ))
    }
  }

  implicit def bolcomParentCategoryPathsWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.ParentCategoryPaths] = new BSONDocumentWriter[com.bol.api.openapi_4_0.ParentCategoryPaths] {
    override def writeTry(ps: ParentCategoryPaths): Try[BSONDocument] = {
      Success(document(
        "parentCategories" -> ps.getParentCategories.asScala,
      ))
    }
  }

  implicit def bolcomOfferDataWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.OfferData] = new BSONDocumentWriter[com.bol.api.openapi_4_0.OfferData] {
    override def writeTry(o: openapi_4_0.OfferData): Try[BSONDocument] = {
      Success(document(
        "bolCom" -> o.getBolCom.toInt,
        "nonProfessionalSellers" -> o.getNonProfessionalSellers.toInt,
        "professionalSellers" -> o.getProfessionalSellers.toInt,
        "offers" -> o.getOffers.asScala
      ))
    }
  }
  implicit def bolcomProductWriter: BSONDocumentWriter[com.bol.api.openapi_4_0.Product] = new BSONDocumentWriter[com.bol.api.openapi_4_0.Product]{
    override def writeTry(p: openapi_4_0.Product): Try[BSONDocument] = {
      Success(document(
        "id" -> p.getId(),
        "ean" -> p.getEAN(),
        "gpc" -> p.getGPC(),
        "title" -> p.getTitle(),
        "specsTag" -> p.getSpecsTag(),
        "summary" -> p.getSummary(),
        "rating" -> p.getRating().toInt,
        "shortDescription" -> p.getShortDescription(),
        "longDescription" -> p.getLongDescription(),
        "trackLists" -> p.getTrackLists().asScala,
        "energyLabelLetter" -> p.getEnergyLabelLetter(),
        "attributeGroups" -> p.getAttributeGroups().asScala.map(ags => document("title" -> ags.getTitle, "attributes" -> ags.getAttributes.asScala.map(a => document("label" -> a.getLabel, "key" -> a.getKey, "value" -> a.getValue)))),
        "entityGroups" -> p.getEntityGroups().asScala.map(es => document("title" -> es.getTitle, ""->es.getEntities.asScala.map(e => document("id"->e.getId,"value"->e.getValue,""->e.getLabel)))),
        "urls" -> p.getUrls().asScala.map(u => document("label" -> u.getLabel, "key" -> u.getKey, "value" -> u.getValue)),
        "images" -> p.getImages().asScala.map(u => document("type" -> u.getType, "key" -> u.getKey, "url" -> u.getUrl)),
        "media" -> p.getMedia().asScala.map(u => document("type" -> u.getType, "key" -> u.getKey, "url" -> u.getUrl)),
        "offerData" -> p.getOfferData(),
        "promotions" -> p.getPromotions().asScala,
        "parentCategoryPaths" -> p.getParentCategoryPaths().asScala,
      ))
    }
  }

  implicit def bolcomProductReader: BSONDocumentReader[com.bol.api.openapi_4_0.Product] = new BSONDocumentReader[com.bol.api.openapi_4_0.Product]() {
    override def readDocument(doc: BSONDocument): Try[openapi_4_0.Product] = ???
  }

  implicit def productInfoWriter: BSONDocumentWriter[ProductInfo] = Macros.writer[ProductInfo]
  implicit def productInfoReader: BSONDocumentReader[ProductInfo] = Macros.reader[ProductInfo]

  // Database and collections: Get references
  val futureConnection: Future[MongoConnection] = parsedUri.flatMap(driver.connect(_))
  def db: Future[DB] = futureConnection.flatMap(_.database("products"))
  def productInfoCollection: Future[BSONCollection] = db.map(_.collection("productInfos"))

  // Write Documents: insert or update


  val productInfoRepository = new ProductInfoRepository {
    override def findByEan(ean: String): Future[Option[ProductInfo]] = {
      productInfoCollection.flatMap(_.find(document("ean" -> ean)). // query builder
        cursor[ProductInfo](). // using the result cursor
        collect[List](1, Cursor.FailOnError[List[ProductInfo]]())).map(_.headOption)
    }

    override def findAll(): Future[List[ProductInfo]] = {
      productInfoCollection.flatMap(_.find(document()). // query builder
        cursor[ProductInfo](). // using the result cursor
        collect[List](-1, Cursor.FailOnError[List[ProductInfo]]()))
    }


    override def upsert(p: ProductInfo): Future[Int] = {
      val selector = document(
        "ean" -> p.ean
      )

      // Update the matching person
      productInfoCollection.flatMap(_.update.one(selector, p,upsert=true).map(_.n))
    }
  }
  // or provide a custom one




  def main(args: Array[String]) = {
    val product = ProductInfo(ean = "12347", title = "Some test product", longDescription="Just for testing")
    (for {
      _ <- productInfoRepository.upsert(product)
      result <- productInfoRepository.findAll()
    } yield result).onComplete(result => {
      println(result)
      System.exit(0)
    })
  }
}
