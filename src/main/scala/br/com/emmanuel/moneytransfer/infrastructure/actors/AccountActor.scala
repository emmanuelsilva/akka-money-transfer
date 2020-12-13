package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain._

object AccountActor {

  def apply(account: Account): Behavior[Command] = {
    Behaviors.setup(context => new AccountActor(context, account))
  }

  final case class AccountWithRef(account: Account, accountRef: ActorRef[Command])

  sealed trait Command
  case class Deposit(amount: BigDecimal) extends Command
  case class Withdraw(amount: BigDecimal, reply: ActorRef[Response]) extends Command
  case class P2PTransfer(amount: BigDecimal, destinationAccount: AccountWithRef, reply: ActorRef[Response]) extends Command
  case class GetBalance(reply : ActorRef[Response]) extends Command
  case class GetTransactions(reply: ActorRef[Response]) extends Command

  sealed trait Response
  case class Balance(account: Account, amount: BigDecimal) extends Response
  case class Transactions(account: Account, transactions: Seq[Transaction]) extends Response
  case class InsufficientFunds(account: Account, command: Command, message: String) extends Response
}

class AccountActor(context: ActorContext[AccountActor.Command], account: Account)
  extends AbstractBehavior[AccountActor.Command](context) {

  import AccountActor._

  var transactions: Seq[Transaction] = Seq[Transaction]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Deposit(amount) => deposit(amount)
      case command: Withdraw => withdraw(command)
      case command: P2PTransfer => p2p(command)
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
    Either.cond(currentBalance >= debitAmount, currentBalance, "Insufficient funds")
  }

  private def p2p(command: P2PTransfer): Unit = {
    hasEnoughBalance(command.amount) match {
      case Left(msg) => command.reply ! InsufficientFunds(account, command, msg)
      case Right(_) => {
        transactions = transactions :+ WithdrawTransaction(account, command.amount)
        command.destinationAccount.accountRef ! Deposit(command.amount)
      }
    }
  }

  private def withdraw(withdraw: Withdraw): Unit = {
    hasEnoughBalance(withdraw.amount) match {
      case Left(msg) => withdraw.reply ! InsufficientFunds(account, withdraw, msg)
      case Right(_) => transactions = transactions :+ WithdrawTransaction(account, withdraw.amount)
    }
  }

  private def deposit(amount: BigDecimal): Unit = {
    transactions = transactions :+ DepositTransaction(account, amount)
  }
}
