package object products {

  case class BookInformation(title: String, description:Option[String] = None, images: Seq[String] = Seq.empty)
}
