package api

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Access some public data on books from LibraryThing
 *
 * @see http://www.librarything.com/api
 */
class LibraryThing(apiKey: String) {
  val pattern = "^\\s*<meta property=\\\"([\\w:]*)\\\" content=\\\"([^\\\"]*)\\\"\\s*/>$".r


  def getMetaData(is: InputStream):Map[String, String] = {
    val reader = new BufferedReader((new InputStreamReader(is)))
    val resp = parseResponse(reader)
      reader.close()
    resp.toMap
  }

  def getMetaDataByISBN(isbn: String): Map[String, String] = {
    val url = new URL(s"http://www.librarything.com/isbn/$isbn")
    val c = url.openConnection()
    val is = c.getInputStream
    val metaData = getMetaData(is)
    is.close()
    metaData
  }

  def parseResponse(reader: BufferedReader):List[(String, String)] = {
    val l = Option(reader.readLine())

    l match {
      case Some(s) if s.trim.startsWith("<link") => parseResponse(reader) //End of header reached, no need to read further
      case Some(s) => {
        pattern.findFirstMatchIn(s).map(m => {
          m.group(1)->m.group(2)
        }).toList ++ parseResponse(reader)
      }
      case _  => List.empty
    }
  }


}

object LibraryThing {
  final def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.global
    val libraryThing = new LibraryThing(System.getenv("LIBRARY_THING_DEV_KEY"))
    if (args.length>0) {
      val isbn = args(0)
      val metadata =libraryThing.getMetaDataByISBN(isbn)
      println(metadata)
    }
  }
}
