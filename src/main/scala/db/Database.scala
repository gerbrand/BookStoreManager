package db
import domain.ProductEntry
import io.getquill._
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

object Database {
  private val logger = LoggerFactory.getLogger(getClass)

  // SnakeCase turns firstName -> first_name
  val ctx = new PostgresJdbcContext(SnakeCase, "testPostgresDB")

  import ctx._

  val flyway: Flyway = Flyway.configure().locations("classpath:db/migration").baselineOnMigrate(true).dataSource(dataSource).load()

  val result = flyway.migrate()
  if (result.targetSchemaVersion!=null)
    logger.info("Database initialized and migrated to {}: {}", result.targetSchemaVersion, result.migrations.asScala.map(_.filepath).mkString(", "))
  else
    logger.info("Database ready, no migrations were needed")

  def main(args: Array[String]): Unit = {

    def getProductEntry(ean: String) = quote {
      query[ProductEntry].filter(p => p.ean == lift(ean))
    }

    def insertPerson(entry: ProductEntry) = quote {
      query[ProductEntry].insert(lift(entry))
    }

    
    val result = run(insertPerson(ProductEntry(ean="12345", reference="testproduct", price=123.0,stock=1,condition="bad",deliveryCode="d",longDescription="Product just for testing")))
    println(result)
    val products: List[ProductEntry] = run(getProductEntry("12345"))
    println(products)
  }
}
