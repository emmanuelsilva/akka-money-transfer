package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol._
import br.com.emmanuel.moneytransfer.domain.ledger.{Account, CreditTransaction, DebitTransaction, Transaction}

import java.util.UUID

object AccountLedgerActor {

  def apply(account: Account): Behavior[Command] = {
    Behaviors.setup(context => new AccountLedgerActor(context, account))
  }
}

class AccountLedgerActor(context: ActorContext[Command], account: Account)
  extends AbstractBehavior[Command](context) {

  var transactions: Seq[Transaction] = Seq[Transaction]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case command: Credit           => credit(command)
      case command: Debit            => debit(command)
      case GetAccountBalance(_, reply)  => reply ! Balance(account, computeCurrentBalance())
      case GetAllTransactions(_, reply) => reply ! Transactions(account, transactions)
    }

    Behaviors.same
  }

  private def computeCurrentBalance(): BigDecimal = {
    transactions.map {
      case credit: CreditTransaction => credit.amount
      case debit: DebitTransaction   => debit.amount * -1
    }.sum
  }

  private def hasEnoughBalance(debitAmount: BigDecimal): Either[String, BigDecimal] = {
    val currentBalance = computeCurrentBalance()
    Either.cond(currentBalance >= debitAmount, currentBalance, "insufficient funds")
  }

  private def debit(command: Debit): Unit = {
    hasEnoughBalance(command.amount) match {
      case Left(_) => command.reply ! InsufficientFunds(account, command)
      case Right(_) =>
        transactions = transactions :+ DebitTransaction(generateTransactionId, command.kind, command.instant, account, command.amount)
        command.reply ! WithdrawConfirmed()
    }
  }

  private def credit(command: Credit): Unit = {
    transactions = transactions :+ CreditTransaction(generateTransactionId, command.kind, command.instant, account, command.amount)
    command.reply ! DepositConfirmed()
  }

  private def generateTransactionId: String = UUID.randomUUID().toString
}
