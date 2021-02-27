package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.Command

import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object BankActor {

  def apply(): Behavior[Command] = {
    Behaviors.setup(context => new BankActor(context))
  }

  sealed trait Command
  case class Deposit(amount: BigDecimal, account: Account, reply: ActorRef[Response]) extends Command
  case class Withdraw(amount: BigDecimal, account: Account, reply: ActorRef[Response]) extends Command
  case class P2P(amount: BigDecimal, from: Account, to: Account, reply: ActorRef[Response]) extends Command
  case class CreateAccount(account: Account) extends Command
  case class GetAccounts(reply: ActorRef[Response]) extends Command
  case class GetAccountBalance(account: Account, reply: ActorRef[Response]) extends Command

  sealed trait Response
  case class Accounts(accounts: immutable.Seq[Account]) extends Response
  case class AccountNotFound(account: Account) extends Response
  case class AccountBalance(account: Account, balance: BigDecimal) extends Response
  case class DepositConfirmed() extends Response
  case class WithdrawConfirmed() extends Response
  case class P2PConfirmed() extends Response
  case class P2PFailed() extends Response
  case class InsufficientFunds(msg: String) extends Response
  case class Failed() extends Response

  sealed trait WrappedMessage extends Command
  private case class WrappedAccountResponse(response: AccountLedgerActor.Response, reply: ActorRef[Response]) extends WrappedMessage
  private case class WrappedP2PResponse(response: AccountLedgerActor.Response, command: P2P) extends WrappedMessage
}

class BankActor(context: ActorContext[BankActor.Command]) extends AbstractBehavior[Command](context) {

  import BankActor._
  implicit val timeout: Timeout = Timeout(1.minute)

  var accounts: Map[Account, ActorRef[AccountLedgerActor.Command]] = Map[Account, ActorRef[AccountLedgerActor.Command]]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case CreateAccount(account)          => createAccount(account)
      case GetAccounts(reply)              => reply ! Accounts(accounts.keys.toSeq)
      case command: GetAccountBalance      => requestAccountBalance(command)
      case command: Deposit                => requestDeposit(command)
      case command: Withdraw               => requestWithdraw(command)
      case command: P2P                    => withdrawFromOriginAccount(command)
      case p2pResponse: WrappedP2PResponse =>
        p2pResponse.response match {
          case AccountLedgerActor.InsufficientFunds(_, _, _) =>
            p2pResponse.command.reply ! P2PFailed()
          case AccountLedgerActor.WithdrawConfirmed() =>
            depositIntoDestinationAccount(p2pResponse.command)
          case AccountLedgerActor.DepositConfirmed() =>
            p2pResponse.command.reply ! P2PConfirmed()
          case AccountLedgerActor.Failed() =>
            p2pResponse.command.reply ! P2PFailed()
        }
      case wrapped: WrappedAccountResponse =>
        wrapped.response match {
          case balance: AccountLedgerActor.Balance =>
            wrapped.reply ! AccountBalance(balance.account, balance.amount)
          case AccountLedgerActor.DepositConfirmed() =>
            wrapped.reply ! DepositConfirmed()
          case AccountLedgerActor.WithdrawConfirmed() =>
            wrapped.reply ! WithdrawConfirmed()
          case AccountLedgerActor.InsufficientFunds(_, _, msg) =>
            wrapped.reply ! InsufficientFunds(msg)
          case AccountLedgerActor.Failed() =>
            wrapped.reply ! Failed()
        }
    }

    Behaviors.same
  }

  private def createAccount(account: Account): Unit = {
    val accountActorRef = context.spawn(AccountLedgerActor(account), account.id)
    accounts += account -> accountActorRef
  }

  private def requestDeposit(command: Deposit): Unit = {
    accounts.get(command.account) match {
      case Some(accountActorRef) =>

        context.ask(accountActorRef, ref => AccountLedgerActor.Deposit(command.amount, ref)) {
          case Success(response) => WrappedAccountResponse(response, command.reply)
          case Failure(_) => WrappedAccountResponse(AccountLedgerActor.Failed(), command.reply)
        }

      case None => command.reply ! AccountNotFound(command.account)
    }
  }

  private def requestWithdraw(command: Withdraw): Unit = {
    accounts.get(command.account) match {
      case Some(accountActorRef) =>

        context.ask(accountActorRef, ref => AccountLedgerActor.Withdraw(command.amount, ref)) {
          case Success(response) => WrappedAccountResponse(response, command.reply)
          case Failure(_) => WrappedAccountResponse(AccountLedgerActor.Failed(), command.reply)
        }

      case None => command.reply ! AccountNotFound(command.account)
    }
  }

  private def requestAccountBalance(getAccountBalance: GetAccountBalance): Unit = {
    val buildAccountResponseMapper = context.messageAdapter {
      response => WrappedAccountResponse(response, getAccountBalance.reply)
    }

    accounts.get(getAccountBalance.account) match {
      case Some(accountActorRef) =>

        context.ask(accountActorRef, AccountLedgerActor.GetBalance) {
          case Success(response) => WrappedAccountResponse(response, getAccountBalance.reply)
          case Failure(_) => WrappedAccountResponse(AccountLedgerActor.Failed(), getAccountBalance.reply)
        }

      case None => getAccountBalance.reply ! AccountNotFound(getAccountBalance.account)
    }
  }

  private def withdrawFromOriginAccount(p2p: P2P): Unit = {
    accounts.get(p2p.to) match {
      case Some(_) => println("Destination account found")
      case None    => p2p.reply ! AccountNotFound(p2p.to)
    }

    accounts.get(p2p.from) match {
      case Some(fromActorRef) =>
        context.ask(fromActorRef, ref => AccountLedgerActor.Withdraw(p2p.amount, ref)) {
          case Success(response) => WrappedP2PResponse(response, p2p)
          case Failure(_) => WrappedP2PResponse(AccountLedgerActor.Failed(), p2p)
        }
      case None => p2p.reply ! AccountNotFound(p2p.from)
    }
  }

  private def depositIntoDestinationAccount(command: P2P): Unit = {
    accounts.get(command.to) match {
      case Some(accountActorRef) =>

        context.ask(accountActorRef, ref => AccountLedgerActor.Deposit(command.amount, ref)) {
          case Success(response) => WrappedP2PResponse(response, command)
          case Failure(_) => WrappedP2PResponse(AccountLedgerActor.Failed(), command)
        }

      case None => command.reply ! AccountNotFound(command.to)
    }
  }
}
