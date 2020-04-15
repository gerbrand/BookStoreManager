package products

import files.BolCom.ProductEntry

import scala.concurrent.Future

trait ProductDAO {
  def upsertProduct(entry: ProductEntry): Future[ProductEntry]
}
