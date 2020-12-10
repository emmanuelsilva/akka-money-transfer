package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain._

import scala.collection.mutable.Seq

object AccountActor {

  def apply(account: Account): Behavior[Command] = {
    Behaviors.setup(context => new AccountActor(context, account))
  }

  final case class AccountWithRef(account: Account, accountRef: ActorRef[Command])

  sealed trait Command
  final case class Deposit(amount: BigDecimal) extends Command
  final case class Withdraw(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  final case class P2PTransfer(amount: BigDecimal, destinationAccount: AccountWithRef, reply: ActorRef[Response]) extends Command
  final case class GetBalance(reply : ActorRef[Response]) extends Command
  final case class GetTransactions(reply: ActorRef[Response]) extends Command

  sealed trait Response
  final case class Balance(account: Account, amount: BigDecimal) extends Response
  final case class Transactions(account: Account, transactions: Seq[Transaction]) extends Response
  final case class InsufficientFunds(account: Account, command: Command) extends Response
}

class AccountActor(context: ActorContext[AccountActor.Command], account: Account)
  extends AbstractBehavior[AccountActor.Command](context) {

  import AccountActor._

  var transactions = Seq[Transaction]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {

      case Deposit(amount) =>
        deposit(amount)
        this

      case command: Withdraw =>
        withdraw(command)
        this

      case command: P2PTransfer =>
        p2p(command)
        this

      case GetBalance(reply) =>
        reply ! Balance(account, computeCurrentBalance())
        this

      case GetTransactions(reply) =>
        reply ! Transactions(account, transactions)
        this
    }
  }

  private def computeCurrentBalance(): BigDecimal = {
    transactions.map(_ match {
      case DepositTransaction(amount) => amount
      case WithdrawTransaction(amount) => amount * -1
      case P2PTransferTransaction(amount, _) => amount * -1
    }).sum
  }

  private def hasEnoughBalance(debitAmount: BigDecimal): Either[String, BigDecimal] = {
    val currentBalance = computeCurrentBalance()

    if (currentBalance < debitAmount) Left("Insufficient funds")
    else Right(currentBalance)
  }

  private def p2p(command: P2PTransfer) = {
    hasEnoughBalance(command.amount) match {
      case Left(_) => command.reply ! InsufficientFunds(account, command)
      case Right(_) => {
        transactions = transactions :+ WithdrawTransaction(command.amount)
        command.destinationAccount.accountRef ! Deposit(command.amount)
      }
    }
  }

  private def withdraw(withdraw: Withdraw) = {
    hasEnoughBalance(withdraw.amount) match {
      case Left(_) => withdraw.reply ! InsufficientFunds(account, withdraw)
      case Right(_) => transactions = transactions :+ WithdrawTransaction(withdraw.amount)
    }
  }

  private def deposit(amount: BigDecimal) = {
    transactions = transactions :+ DepositTransaction(amount)
  }
}
