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
  case class Deposit(amount: BigDecimal, account: Account) extends Command
  case class CreateAccount(account: Account) extends Command
  case class GetAccounts(reply: ActorRef[Response]) extends Command

  sealed trait Response
  case class Accounts(accounts: immutable.Seq[Account]) extends Response
  case class AccountCreated() extends Response
  case class AccountAlreadyExists() extends Response
}

class BankActor(context: ActorContext[BankActor.Command]) extends AbstractBehavior[Command](context) {

  import BankActor._

  var accounts = Map[Account, ActorRef[AccountActor.Command]]()

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case CreateAccount(account) => createAccount(account)
      case GetAccounts(reply) => reply ! Accounts(accounts.keys.toSeq)
    }
    this
  }

  private def createAccount(account: Account) = {
    val accountActorRef = context.spawn(AccountActor(account), account.id)
    accounts += account -> accountActorRef
  }
}
