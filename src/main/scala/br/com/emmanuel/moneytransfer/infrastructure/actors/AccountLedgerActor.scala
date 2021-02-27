package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain._

object AccountLedgerActor {

  def apply(account: Account): Behavior[Command] = {
    Behaviors.setup(context => new AccountLedgerActor(context, account))
  }

  sealed trait Command
  case class Deposit(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  case class Withdraw(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  case class GetBalance(reply : ActorRef[Response]) extends Command
  case class GetTransactions(reply: ActorRef[Response]) extends Command

  sealed trait Response
  case class Balance(account: Account, amount: BigDecimal) extends Response
  case class Transactions(account: Account, transactions: Seq[Transaction]) extends Response
  case class InsufficientFunds(account: Account, command: Command, message: String) extends Response
  case class DepositConfirmed() extends Response
  case class WithdrawConfirmed() extends Response
  case class Failed() extends Response
}

class AccountLedgerActor(context: ActorContext[AccountLedgerActor.Command], account: Account)
  extends AbstractBehavior[AccountLedgerActor.Command](context) {

  import AccountLedgerActor._

  var transactions: Seq[Transaction] = Seq[Transaction]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case command: Deposit => deposit(command)
      case command: Withdraw => withdraw(command)
      case GetBalance(reply) => reply ! Balance(account, computeCurrentBalance())
      case GetTransactions(reply) => reply ! Transactions(account, transactions)
    }

    this
  }

  private def computeCurrentBalance(): BigDecimal = {
    transactions.map {
      case credit: CreditTransaction => credit.amount
      case debit: DebitTransaction => debit.amount * -1
    }.sum
  }

  private def hasEnoughBalance(debitAmount: BigDecimal): Either[String, BigDecimal] = {
    val currentBalance = computeCurrentBalance()
    Either.cond(currentBalance >= debitAmount, currentBalance, "Can't complete the withdraw due to insufficient funds")
  }

  private def withdraw(command: Withdraw): Unit = {
    hasEnoughBalance(command.amount) match {
      case Left(msg) => command.reply ! InsufficientFunds(account, command, msg)
      case Right(_) =>
        transactions = transactions :+ WithdrawTransaction(account, command.amount)
        command.reply ! WithdrawConfirmed()
    }
  }

  private def deposit(command: Deposit): Unit = {
    transactions = transactions :+ DepositTransaction(account, command.amount)
    command.reply ! DepositConfirmed()
  }
}
