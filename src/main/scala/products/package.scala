import java.net.URL

package object products {

  case class BookInformation(title: Option[String], description:Option[String] = None, images: Seq[URL] = Seq.empty)
}
