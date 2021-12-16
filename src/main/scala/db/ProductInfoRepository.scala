package db

import domain.ProductInfo

import scala.concurrent.Future

trait ProductInfoRepository {
  def findByEan(ean: String): Future[Option[ProductInfo]]
  def upsert(p: ProductInfo): Future[Int]
  def findAll(): Future[List[ProductInfo]]
}
