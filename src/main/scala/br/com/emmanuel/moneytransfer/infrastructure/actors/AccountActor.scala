package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain._

import scala.collection.mutable.Seq

object AccountActor {

  def apply(account: Account): Behavior[Command] = {
    Behaviors.setup(context => new AccountActor(context, account))
  }

  final case class AccountRef(account: Account, accountRef: ActorRef[Command])

  sealed trait Command
  final case class Deposit(amount: BigDecimal) extends Command
  final case class Withdraw(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  final case class P2PTransfer(amount: BigDecimal, destinationAccount: AccountRef, reply: ActorRef[Response]) extends Command
  final case class GetBalance(reply : ActorRef[Response]) extends Command

  sealed trait Response
  final case class Balance(account: Account, amount: BigDecimal) extends Response
  final case class InsufficientFunds(account: Account, command: Command) extends Response
}

class AccountActor(context: ActorContext[AccountActor.Command], account: Account)
  extends AbstractBehavior[AccountActor.Command](context) {

  import AccountActor._

  var transactions = Seq[Transaction]()

  def computeCurrentBalance(): BigDecimal = {
    transactions.map(_ match {
      case DepositTransaction(amount) => amount
      case WithdrawTransaction(amount) => amount * -1
      case P2PTransferTransaction(amount, _) => amount * -1
    }).sum
  }

  def hasEnoughBalance(debitAmount: BigDecimal): Either[String, BigDecimal] = {
    val currentBalance = computeCurrentBalance()

    if (currentBalance < debitAmount) Left("Insufficient funds")
    else Right(currentBalance)
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {

      case Deposit(amount) =>
        deposit(amount)
        this

      case Withdraw(amount, reply) =>
        withdraw(msg, amount, reply)
        this

      case p2pCommand: P2PTransfer =>
        p2p(p2pCommand)
        this

      case GetBalance(reply) =>
        val currentBalance = computeCurrentBalance()
        reply ! Balance(account, currentBalance)
        this
    }
  }

  private def p2p(p2pCommand: P2PTransfer) = {
    hasEnoughBalance(p2pCommand.amount) match {
      case Left(_) => p2pCommand.reply ! InsufficientFunds(account, p2pCommand)
      case Right(_) => {
        transactions = transactions :+ WithdrawTransaction(p2pCommand.amount)
        p2pCommand.destinationAccount.accountRef ! Deposit(p2pCommand.amount)
      }
    }
  }

  private def withdraw(msg: Command, amount: BigDecimal, reply: ActorRef[Response]) = {
    hasEnoughBalance(amount) match {
      case Left(_) => reply ! InsufficientFunds(account, msg)
      case Right(_) => transactions = transactions :+ WithdrawTransaction(amount)
    }
  }

  private def deposit(amount: BigDecimal) = {
    transactions = transactions :+ DepositTransaction(amount)
  }
}
