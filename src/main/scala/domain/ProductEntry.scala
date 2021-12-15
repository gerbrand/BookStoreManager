package domain

import java.net.URL

case class ProductEntry(reference: String, ean: String, condition: String, stock: Int = 0, price: BigDecimal, deliveryCode: String, description: Option[String] = None, longDescription: String, forSale: Boolean = false, title: Option[String] = None, images: Option[Seq[String]] = None)

