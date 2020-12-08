package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain.{DepositTransaction, Transaction, WithdrawTransaction}

import scala.collection.mutable.Seq

object AccountActor {

  def apply(accountId: String): Behavior[Command] = {
    Behaviors.setup(context => new AccountActor(context, accountId))
  }

  sealed trait Command
  final case class Deposit(amount: BigDecimal) extends Command
  final case class Withdraw(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  final case class GetBalance(reply : ActorRef[Response]) extends Command

  sealed trait Response
  final case class Balance(accountId: String, amount: BigDecimal) extends Response
  final case class InsufficientFunds(accountId: String, command: Command) extends Response
}

class AccountActor(context: ActorContext[AccountActor.Command], accountId: String)
  extends AbstractBehavior[AccountActor.Command](context) {

  import AccountActor._

  var transactions = Seq[Transaction]()

  def getBalance(): BigDecimal = {
    transactions.map(_ match {
      case DepositTransaction(amount) => amount
      case WithdrawTransaction(amount) => amount * -1
    }).sum
  }

  def hasEnoughBalance(debitAmount: BigDecimal): Boolean = {
    getBalance() >= debitAmount
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Deposit(amount) =>
        transactions = transactions :+ DepositTransaction(amount)
        this

      case Withdraw(amount, reply) =>
        if (hasEnoughBalance(amount))
          transactions = transactions :+ WithdrawTransaction(amount)
        else
          reply ! InsufficientFunds(accountId, msg)

        this

      case GetBalance(reply) =>
        val balance = getBalance()
        reply ! Balance(accountId, balance)
        this
    }
  }
}
