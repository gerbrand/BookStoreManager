package products

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

@Singleton
class ProductService @Inject()(dao: ProductDAO) {
  private val log = LoggerFactory.getLogger(getClass)
}
