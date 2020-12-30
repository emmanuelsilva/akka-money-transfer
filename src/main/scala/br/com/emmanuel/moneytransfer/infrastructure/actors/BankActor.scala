package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.Command

import scala.collection.immutable

object BankActor {

  def apply(): Behavior[Command] = {
    Behaviors.setup(context => new BankActor(context))
  }

  sealed trait Command
  case class Deposit(amount: BigDecimal, account: Account, reply: ActorRef[Response]) extends Command
  case class Withdraw(amount: BigDecimal, account: Account, reply: ActorRef[Response]) extends Command
  case class CreateAccount(account: Account) extends Command
  case class GetAccounts(reply: ActorRef[Response]) extends Command
  case class GetAccountBalance(account: Account, reply: ActorRef[Response]) extends Command

  sealed trait Response
  case class Accounts(accounts: immutable.Seq[Account]) extends Response
  case class AccountNotFound(account: Account) extends Response
  case class AccountBalance(account: Account, balance: BigDecimal) extends Response
  case class DepositConfirmed() extends Response
  case class WithdrawConfirmed() extends Response
  case class InsufficientFounds(msg: String) extends Response

  sealed trait WrappedMessage extends Command
  private case class WrappedAccountResponse(response: AccountLedgerActor.Response, reply: ActorRef[Response]) extends WrappedMessage
}

class BankActor(context: ActorContext[BankActor.Command]) extends AbstractBehavior[Command](context) {

  import BankActor._

  var accounts: Map[Account, ActorRef[AccountLedgerActor.Command]] = Map[Account, ActorRef[AccountLedgerActor.Command]]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case CreateAccount(account)          => createAccount(account)
      case GetAccounts(reply)              => reply ! Accounts(accounts.keys.toSeq)
      case command: GetAccountBalance      => requestAccountBalance(command)
      case command: Deposit                => requestDeposit(command)
      case command: Withdraw               => requestWithdraw(command)
      case wrapped: WrappedAccountResponse =>
        wrapped.response match {
          case balance: AccountLedgerActor.Balance =>
            wrapped.reply ! AccountBalance(balance.account, balance.amount)
          case AccountLedgerActor.DepositConfirmed() =>
            wrapped.reply ! DepositConfirmed()
          case AccountLedgerActor.WithdrawConfirmed() =>
            wrapped.reply ! WithdrawConfirmed()
          case AccountLedgerActor.InsufficientFunds(_, _, msg) =>
            wrapped.reply ! InsufficientFounds(msg)
        }
    }

    this
  }

  private def createAccount(account: Account): Unit = {
    val accountActorRef = context.spawn(AccountLedgerActor(account), account.id)
    accounts += account -> accountActorRef
  }

  private def requestDeposit(command: Deposit): Unit = {
    val buildAccountResponseMapper = context.messageAdapter {
      response => WrappedAccountResponse(response, command.reply)
    }

    accounts.get(command.account) match {
      case Some(accountActorRef) => accountActorRef ! AccountLedgerActor.Deposit(command.amount, buildAccountResponseMapper)
      case None => command.reply ! AccountNotFound(command.account)
    }
  }

  private def requestWithdraw(command: Withdraw): Unit = {
    val buildAccountResponseMapper = context.messageAdapter {
      response => WrappedAccountResponse(response, command.reply)
    }

    accounts.get(command.account) match {
      case Some(accountActorRef) => accountActorRef ! AccountLedgerActor.Withdraw(command.amount, buildAccountResponseMapper)
      case None => command.reply ! AccountNotFound(command.account)
    }
  }

  private def requestAccountBalance(getAccountBalance: GetAccountBalance): Unit = {
    val buildAccountResponseMapper = context.messageAdapter {
      response => WrappedAccountResponse(response, getAccountBalance.reply)
    }

    accounts.get(getAccountBalance.account) match {
      case Some(accountActorRef) => accountActorRef ! AccountLedgerActor.GetBalance(buildAccountResponseMapper)
      case None => getAccountBalance.reply ! AccountNotFound(getAccountBalance.account)
    }
  }
}
