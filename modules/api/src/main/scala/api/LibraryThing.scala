package api

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL

import scala.util.{Failure, Success, Try}

/**
 * Access some public data from LibraryThing
 *
 * @see http://www.librarything.com/api
 */
object LibraryThing {
  val pattern = "^\\s*<meta property=\\\"([\\w:]*)\\\" content=\\\"([^\\\"]*)\\\"\\s*/>$".r


  val libraryThingDevKey = System.getenv("LIBRARY_THING_DEV_KEY")

  def getMetaData(is: InputStream):Map[String, String] = {
    val reader = new BufferedReader((new InputStreamReader(is)))
    val resp = parseResponse(reader)
      reader.close()
    resp.toMap
  }

  def getMetaDataByISBN(isbn: String): Option[Map[String, String]] = {
    Try({
      val url = new URL(s"http://www.librarything.com/isbn/$isbn")
      val c = url.openConnection()
      val is = c.getInputStream
      val metaData = getMetaData(is)
      is.close()
      metaData
    }) match {
      case Success(m) if (m.isEmpty) => None
      case Success(m) => Some(m)
      case Failure(e) => {
        e.printStackTrace()
        None
      }
    }
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

  final def main(args: Array[String]) = {
    if (args.length>0) {
      val isbn = args(0)
      val metadata = LibraryThing.getMetaDataByISBN(isbn)
      println(metadata)
    }
  }
}
