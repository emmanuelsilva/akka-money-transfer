package br.com.emmanuel.moneytransfer.infrastructure.actors

import scala.collection.mutable.Seq
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import br.com.emmanuel.moneytransfer.domain.entity.{DepositTransaction, Transaction}

object AccountActor {

  def apply(accountId: String): Behavior[Command] = {
    Behaviors.setup(context => new AccountActor(context, accountId))
  }

  sealed trait Command
  final case class Deposit(amount: BigDecimal) extends Command
  final case class GetBalance(reply : ActorRef[Response]) extends Command

  sealed trait Response
  final case class Balance(accountId: String, amount: BigDecimal) extends Response
}

class AccountActor(context: ActorContext[AccountActor.Command], accountId: String)
  extends AbstractBehavior[AccountActor.Command](context) {

  import AccountActor._

  var transactions = Seq[Transaction]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Deposit(amount) =>
        transactions = transactions :+ DepositTransaction(amount)
        this

      case GetBalance(reply) =>
        val balance = transactions.map(_.amount).sum
        reply ! Balance(accountId, balance)
        this
    }
  }
}
