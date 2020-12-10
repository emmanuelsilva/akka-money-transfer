package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.Accounts
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object HttpRestServer extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val system: ActorSystem[BankActor.Command] = ActorSystem(BankActor(), "bank")
  implicit val executionContext: ExecutionContext = system.executionContext

  implicit val accountJsonFormat = jsonFormat1(Account)
  implicit val accountsJsonFormat = jsonFormat1(Accounts)

  def main(args: Array[String]): Unit = {

    val bankActor = system

    val route = {
      concat(AccountRoute.route(bankActor))
    }

    val server = Http()
      .newServerAt("localhost", 8080)
      .bind(route)

    println("Server started...")
    StdIn.readLine()

    server.onComplete(_ => system.terminate())

  }

}
