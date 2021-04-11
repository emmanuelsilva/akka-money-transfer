package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import br.com.emmanuel.moneytransfer.infrastructure.actors.factory.ShardingAccountEntityFactory
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.Success

object HttpRestServer extends SprayJsonSupport with DefaultJsonProtocol {

  def init(context: ActorContext[Nothing], sharding: ClusterSharding): Unit = {
    implicit val system = context.system
    implicit val executionContext: ExecutionContext = context.executionContext

    val route = {
      concat(AccountRoute.route(ShardingAccountEntityFactory(sharding)))
    }

    println("HTTP server starting...")

    Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(route)
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info(s"HTTP server started at ${address.getHostName} at ${address.getPort}")
          system.log.info(s"address=$address")
      }

    StdIn.readLine()


  }
}
