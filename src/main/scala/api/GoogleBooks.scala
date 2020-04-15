package api

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.books.Books
import com.google.api.services.books.BooksRequestInitializer
import com.google.api.services.books.model.Volume
import com.google.api.services.books.model.Volumes
import java.io.IOException
import java.net.URLEncoder
import java.text.NumberFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/*
 * Based on https://github.com/google/google-api-java-client-samples/blob/master/books-cmdline-sample/src/main/java/com/google/api/services/samples/books/cmdline/ ( Copyright (c) 2011 Google Inc. )
 */
object GoogleBooks {

  val jsonFactory = JacksonFactory.getDefaultInstance()

  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or
   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private val APPLICATION_NAME = "SoftwareCreation-TinyBookStoremanager/0.1"

  private val CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance
  private val PERCENT_FORMATTER = NumberFormat.getPercentInstance

  ClientCredentials.errorIfNotSpecified
  // Set up Books client.
  import api.ClientCredentials
  import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
  import com.google.api.services.books.Books
  import com.google.api.services.books.BooksRequestInitializer
  val books = new Books.Builder(GoogleNetHttpTransport.newTrustedTransport, jsonFactory, null).setApplicationName(APPLICATION_NAME).setGoogleClientRequestInitializer(new BooksRequestInitializer(ClientCredentials.API_KEY)).build

  def queryGoogleBooks(query: String)(implicit ec:ExecutionContext): Future[List[Volume]] = {

    Future {
      //val q = s"isbn:$query"
      val q = s"isbn:$query"

      val volumesList = books.volumes().list(q)
      val result = volumesList.execute()
      if (result.getTotalItems>0) {
        result.getItems.asScala.toList
      } else {
        List.empty
      }
    }
  }
}
