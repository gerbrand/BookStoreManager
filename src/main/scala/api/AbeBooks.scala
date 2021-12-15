package api

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Access publicly available data on books from AbeBooks
 */
object AbeBooks {
  def getImageUrl(isbn: String) = {
    val abeBookUrl = new URL(s"https://pictures.abebooks.com/isbn/${isbn}-us-300.jpg")
    val contentType = abeBookUrl.openConnection().getContentType
    if (contentType!=null && contentType.startsWith("image")) {
      Some(abeBookUrl)
    } else {
      None
    }
  }
}
