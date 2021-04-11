package br.com.emmanuel.moneytransfer

import akka.actor.typed.ActorSystem
import br.com.emmanuel.moneytransfer.infrastructure.actors.BootstrapActor


object Run extends App {

  val system = ActorSystem[Nothing](BootstrapActor(), "ledger")

}
