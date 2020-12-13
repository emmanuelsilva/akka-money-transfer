package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import br.com.emmanuel.moneytransfer.domain.{Account, DepositTransaction}
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.{AccountBalance, Accounts}
import spray.json.DefaultJsonProtocol

trait HasJsonSerializer extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val accountJsonFormat = jsonFormat1(Account)
  implicit val accountsJsonFormat = jsonFormat1(Accounts)
  implicit val accountBalanceJsonFormat = jsonFormat2(AccountBalance)

  implicit val depositTransactionJsonFormat =
    jsonFormat[Account, BigDecimal, DepositTransaction](DepositTransaction, "account", "amount")
}
