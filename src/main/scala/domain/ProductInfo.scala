package domain

import reactivemongo.api.bson.BSONDocument

case class ProductInfo(ean: String, title: String, description: Option[String] = None, longDescription: String, images: Option[Seq[String]] = None, bolcomInfo: Option[com.bol.api.openapi_4_0.Product]=None )
