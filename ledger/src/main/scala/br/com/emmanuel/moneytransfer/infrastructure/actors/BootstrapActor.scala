package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Subscribe}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor
import br.com.emmanuel.moneytransfer.infrastructure.rest.HttpRestServer

object BootstrapActor {

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val (_, sharding) = startCluster(context)
    startClusterBootstrap(context)
    startLedgerApplication(context, sharding)

    Behaviors.empty
  }

  private def startLedgerApplication(context: ActorContext[Nothing], sharding: ClusterSharding): Unit = {
    AccountLedgerActor.configureSharding(sharding)
    HttpRestServer.init(context, sharding)
  }

  private def startCluster(context: ActorContext[Nothing]): (Cluster, ClusterSharding) = {
    val cluster = Cluster(context.system)
    val sharding = ClusterSharding(context.system)

    val listener = context.spawn(clusterMemberListener(cluster), "memberListener")
    cluster.subscriptions ! Subscribe(listener, classOf[MemberEvent])

    (cluster, sharding)
  }

  private def startClusterBootstrap(context: ActorContext[Nothing]): Unit = {
    AkkaManagement(context.system).start()
    ClusterBootstrap(context.system).start()
  }

  private def clusterMemberListener(cluster: Cluster): Behaviors.Receive[MemberEvent] = Behaviors.receive[MemberEvent] { (context, msg) =>
    msg match {
      case MemberUp(member) =>
        println(s"new member-up=$member")

        if (member.uniqueAddress eq cluster.selfMember.uniqueAddress) {
          println("btw, I am UP :) ")
        } else {
          println(s"${member} is not equals to ${cluster.selfMember}")
        }

        Behaviors.same
      case event: MemberEvent =>
        print(s"cluster-event=${event}")
        Behaviors.same
    }
  }
}