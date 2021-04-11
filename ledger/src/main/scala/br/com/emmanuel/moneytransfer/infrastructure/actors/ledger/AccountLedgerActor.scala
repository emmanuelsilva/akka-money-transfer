package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import br.com.emmanuel.moneytransfer.domain.{Account, ClosedAccount, EmptyAccount, OpenedAccount}
import br.com.emmanuel.moneytransfer.event.AccountEvent._
import br.com.emmanuel.moneytransfer.event.Event
import br.com.emmanuel.moneytransfer.infrastructure.serialization.SerializableMessage

import java.util.Calendar

object AccountLedgerActor {

  // sharding
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("account-ledger")

  type ReplyActorType = ActorRef[StatusReply[Done]]

  sealed trait HasReply {
    def replyTo: ReplyActorType
  }

  //commands
  sealed trait Command extends SerializableMessage

  //write commands
  final case class OpenAccount(replyTo: ReplyActorType) extends Command with HasReply
  final case class CloseAccount(replyTo: ReplyActorType) extends Command with HasReply
  final case class Credit(kind: String, instant: Calendar, amount: BigDecimal, replyTo: ReplyActorType) extends Command with HasReply
  final case class Debit(kind: String, instant: Calendar, amount: BigDecimal, replyTo: ReplyActorType) extends Command with HasReply

  //read commands
  final case class GetBalance(replyTo: ActorRef[Reply]) extends Command

  //response
  sealed trait Reply extends SerializableMessage
  final case class CurrentBalance(balance: BigDecimal) extends Reply

  def configureSharding(sharding: ClusterSharding): ActorRef[ShardingEnvelope[Command]] = {
    sharding.init(
      Entity(typeKey = TypeKey)
      (createBehavior = entityContext => AccountLedgerActor(entityContext.entityId))
    )
  }

  def apply(accountId: String): Behavior[Command] = EventSourcedBehavior[Command, Event, Account](
    persistenceId = PersistenceId(TypeKey.name, accountId),
    emptyState = EmptyAccount(accountId),
    commandHandler = commandHandler(accountId),
    eventHandler = eventHandler
  )

  def commandHandler(accountId: String): (Account, Command) => Effect[Event, Account] = { (state, command) =>

    state match {
      case EmptyAccount(_) =>
        command match {
          case cmd: OpenAccount  => openAccount(cmd)
          case cmd: GetBalance   => Effect.reply(cmd.replyTo)(CurrentBalance(0))
          case cmd: CloseAccount => Effect.persist(AccountClosed).thenReply(cmd.replyTo)(_ => StatusReply.Ack)
          case cmd: HasReply     => Effect.reply(cmd.replyTo)(StatusReply.Error(s"account=$accountId is not opened"))
          case _                 => Effect.unhandled.thenNoReply()
        }

      case account: OpenedAccount =>
        command match {
          case cmd: Credit        => credit(cmd)
          case cmd: Debit         => debit(account, cmd)
          case cmd: CloseAccount  => closeAccount(account, cmd)
          case cmd: GetBalance    => getBalance(account, cmd)
          case cmd: OpenAccount   => Effect.reply(cmd.replyTo)(StatusReply.Error(s"account=$accountId is already opened"))
        }

      case ClosedAccount(_) =>
        command match {
          case cmd: GetBalance   => Effect.reply(cmd.replyTo)(CurrentBalance(0))
          case cmd: HasReply     => Effect.reply(cmd.replyTo)(StatusReply.Error(s"account=$accountId is already close"))
        }
    }
  }

  val eventHandler: (Account, Event) => Account = { (account, event) =>
    account.applyEvent(event)
  }

  private def openAccount(cmd: OpenAccount): Effect[Event, Account] = {
    Effect.persist(AccountOpened).thenReply(cmd.replyTo)(_ => StatusReply.Ack)
  }

  private def credit(cmd: Credit): Effect[Event, Account] = {
    Effect.persist(Deposited(cmd.kind, cmd.instant, cmd.amount)).thenReply(cmd.replyTo)(_ => StatusReply.Ack)
  }

  private def debit(openedAccount: OpenedAccount, cmd: Debit): Effect[Event, Account] = {
    if (openedAccount.canDebit(cmd.amount)) {
      Effect.persist(Debited(cmd.kind, cmd.instant, cmd.amount)).thenReply(cmd.replyTo)(_ => StatusReply.Ack)
    } else {
      Effect.reply(cmd.replyTo)(StatusReply.Error(s"insufficient balance to debit ${cmd.amount}"))
    }
  }

  private def closeAccount(account: OpenedAccount, cmd: CloseAccount): Effect[Event, Account] = {
    if (account.canClose) {
      Effect.persist(AccountClosed).thenReply(cmd.replyTo)(_ => StatusReply.Ack)
    } else {
      Effect.reply(cmd.replyTo)(StatusReply.Error(s""))
    }
  }

  private def getBalance(acc: OpenedAccount, cmd: GetBalance): Effect[Event, Account] = {
    Effect.reply(cmd.replyTo)(CurrentBalance(acc.balance))
  }

}
