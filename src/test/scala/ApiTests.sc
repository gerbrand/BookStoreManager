import java.net.URL

val abeBookUrl = new URL(s"https://pictures.abebooks.com/isbn/9783453023888-us-300.jpg")
//val c = abeBookUrl.openConnection()
//c.getContentType


val pattern = "^\\s*<meta property=\\\"([\\w:]*)\\\" content=\\\"([^\\\"]*)\\\"\\s*/>$".r

pattern.findAllMatchIn("    <meta property=\"og:image\" content=\"http://ecx.images-amazon.com/images/P/3791302930.01._SS250_SS250_SCLZZZZZZZ_.jpg\"   />")

LibraryThing.getMetaDataByISBN("0441172717")