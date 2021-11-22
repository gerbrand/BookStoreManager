package api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  // Defining the format. Not too many boiler plate using spray

  // From https://github.com/spray/spray-json#providing-jsonformats-for-unboxed-types
//  implicit object CurrencyFormat extends JsonFormat[Currency] {
//    val fmt = """([A-Z]{3})""".r
//    def write(m: Currency) = JsString(s"${m.currencyCode}")
//    def read(json: JsValue) = json match {
//      case JsString(fmt(c)) => Currency(c)
//      case _ => deserializationError("Valid currency code expected")
//    }
//  }
//  implicit val exchangeRateRequestFormat = jsonFormat3(ExchangeRateRequest)
//  implicit val exchangeRateResponse = jsonFormat3(ExchangeRateResponse)
}
