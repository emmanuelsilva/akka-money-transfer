package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.Accounts
import spray.json.DefaultJsonProtocol

trait HasAccountJsonSerializer extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val accountJsonFormat = jsonFormat1(Account)
  implicit val accountsJsonFormat = jsonFormat1(Accounts)
}
