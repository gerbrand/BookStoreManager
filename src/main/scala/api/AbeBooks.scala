package api

import java.net.URL

import scala.concurrent.{ExecutionContext, Future}

object AbeBooks {
  def getImageUrl(isbn: String)(implicit ec: ExecutionContext) = {
    Future {
      val abeBookUrl = new URL(s"https://pictures.abebooks.com/isbn/${isbn}-us-300.jpg")
      val contentType = abeBookUrl.openConnection().getContentType
      if (contentType.startsWith("image")) {
        Some(abeBookUrl.toExternalForm)
      } else {
        None
      }
    }
  }
}
