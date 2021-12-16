package domain

case class ProductInfo(ean: String, title: String, description: Option[String] = None, longDescription: String, images: Option[Seq[String]] = None )
