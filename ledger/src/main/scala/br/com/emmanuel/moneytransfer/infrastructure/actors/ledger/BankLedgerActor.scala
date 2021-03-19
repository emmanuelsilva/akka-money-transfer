package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import br.com.emmanuel.moneytransfer.domain.ledger.Account
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol._

object BankLedgerActor {

  def apply(): Behavior[Command] = {
    Behaviors.setup(context => new BankLedgerActor(context))
  }
}

class BankLedgerActor(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  var accounts = Map[Account, ActorRef[Command]]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case command: CreateAccount     => createAccount(command)
      case command: GetAccounts       => getAccounts(command)
      case command: Credit            => forwardToAccount(command.account, command)
      case command: Debit             => forwardToAccount(command.account, command)
      case command: GetAccountBalance => forwardToAccount(command.account, command)
    }

    Behaviors.same
  }

  private def createAccount(command: CreateAccount): Unit = {
    val account = command.account
    val accountActorRef = context.spawn(AccountLedgerActor(account), account.id)
    accounts += account -> accountActorRef
  }

  private def getAccounts(command: GetAccounts) = {
    command.reply ! Accounts(accounts.keys.toSeq)
  }

  private def forwardToAccount(account: Account, command: CommandWithReply) = {
    accounts.get(account) match {
      case Some(accountLedgerActor) => accountLedgerActor ! command
      case None                     => command.reply ! AccountNotFound(account)
    }
  }
}
