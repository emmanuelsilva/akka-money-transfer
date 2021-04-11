package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor.CurrentBalance
import br.com.emmanuel.moneytransfer.infrastructure.rest.request.{AccountRequest, CreditTransactionRequest, DebitTransactionRequest}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import java.text.SimpleDateFormat
import java.util.{Calendar, GregorianCalendar}

object CalendarMarshalling {

  implicit object CalendarJsonFormat extends JsonFormat[Calendar] {

    val calendarFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override def write(obj: Calendar): JsValue = JsString(calendarFormatter.format(obj.getTime))

    override def read(json: JsValue): Calendar = {
      val time = calendarFormatter.parse(json.asInstanceOf[JsString].value)
      val calendar = new GregorianCalendar()
      calendar.setTime(time)
      calendar
    }

  }
}

trait HasJsonSerializer extends SprayJsonSupport with DefaultJsonProtocol {

  import CalendarMarshalling._

  implicit val accountJsonFormat = jsonFormat1(AccountRequest)
  implicit val currentBalanceJsonFormat = jsonFormat1(CurrentBalance)

  implicit val creditTransactionRequestJsonFormat = {
    jsonFormat[String, String, Calendar, BigDecimal, CreditTransactionRequest](CreditTransactionRequest, "id", "kind", "instant", "amount")
  }

  implicit val debitTransactionRequestJsonFormat =
    jsonFormat[String, String, Calendar, BigDecimal, DebitTransactionRequest](DebitTransactionRequest, "id", "kind", "instant", "amount")
}
