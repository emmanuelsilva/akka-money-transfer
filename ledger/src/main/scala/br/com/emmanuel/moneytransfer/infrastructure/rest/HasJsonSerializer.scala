package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol.{Balance, Accounts}
import br.com.emmanuel.moneytransfer.domain.ledger.{Account, CreditTransaction, DebitTransaction}
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

  implicit val accountJsonFormat = jsonFormat1(Account)
  implicit val accountsJsonFormat = jsonFormat1(Accounts)
  implicit val accountBalanceJsonFormat = jsonFormat2(Balance)


  implicit val creditTransactionJsonFormat = {
    jsonFormat[String, String, Calendar, Account, BigDecimal, CreditTransaction](CreditTransaction, "id", "kind", "instant", "account", "amount")
  }

  implicit val debitTransactionJsonFormat =
    jsonFormat[String, String, Calendar, Account, BigDecimal, DebitTransaction](DebitTransaction, "id", "kind", "instant", "account", "amount")
}
