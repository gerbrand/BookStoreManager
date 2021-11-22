package api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class WooCommerceClient(wpRestUrl: String, key: String, secret: String)(implicit val system:ActorSystem, implicit val executionContext: ExecutionContext) {
  val request = Get(s"${wpRestUrl}/wc/v3/orders")
    .addCredentials(BasicHttpCredentials(key, secret))
  val fResult = Http().singleRequest(request).transform(res => Unmarshal(res).to[String], e => e).flatten
  val result = Await.result(fResult, 5.seconds);
  println(result)
  system.terminate()

}
object WooCommerceClient extends App {
  implicit val system: ActorSystem = ActorSystem("akka-http-sample")
  sys.addShutdownHook(system.terminate())
  implicit val executionContext = system.getDispatcher

  val client = new WooCommerceClient(System.getenv("WOOCOMMERCE_REST_URL"), System.getenv("WOOCOMMERCE_PUBIC_KEY"), System.getenv("WOOCOMMERCE_SECRET_KEY"))
}
