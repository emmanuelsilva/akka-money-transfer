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

object AccountLedgerEntityActor {

  // sharding
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("account-ledger")

  type ReplyActorType = ActorRef[StatusReply[Done]]

  //commands
  sealed trait Command extends SerializableMessage

  //write commands
  final case class OpenAccount(replyTo: ReplyActorType) extends Command with SerializableMessage
  final case class CloseAccount(replyTo: ReplyActorType) extends Command
  final case class Credit(kind: String, instant: Calendar, amount: BigDecimal, replyTo: ReplyActorType) extends Command
  final case class Debit(kind: String, instant: Calendar, amount: BigDecimal, replyTo: ReplyActorType) extends Command

  //read commands
  final case class GetBalance(replyTo: ActorRef[Reply]) extends Command

  //response
  sealed trait Reply extends SerializableMessage
  final case class CurrentBalance(balance: BigDecimal) extends Reply

  def configureSharding(sharding: ClusterSharding): ActorRef[ShardingEnvelope[Command]] = {
    sharding.init(
      Entity(typeKey = TypeKey)
      (createBehavior = entityContext => AccountLedgerEntityActor(entityContext.entityId))
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
      case _: EmptyAccount =>
        command match {
          case o: OpenAccount => openAccount(o)
          case c: Credit      => Effect.reply(c.replyTo)(StatusReply.Error(s"account=$accountId is not opened"))
          case _              => Effect.unhandled.thenNoReply()
        }

      case account: OpenedAccount =>
        command match {
          case c: Credit        => credit(c)
          case d: Debit         => debit(account, d)
          case c: CloseAccount  => closeAccount(account, c)
          case g: GetBalance    => getBalance(account, g)
          case c: OpenAccount => Effect.reply(c.replyTo)(StatusReply.Error(s"account=$accountId is already opened"))
        }

      case _: ClosedAccount =>
        command match {
          case c: Credit       => Effect.reply(c.replyTo)(StatusReply.Error(s"account=$accountId is already close"))
          case d: Debit        => Effect.reply(d.replyTo)(StatusReply.Error(s"account=$accountId is already close"))
          case c: CloseAccount => Effect.reply(c.replyTo)(StatusReply.Error(s"account=$accountId is already close"))
          case g: GetBalance   => Effect.reply(g.replyTo)(CurrentBalance(0))
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
