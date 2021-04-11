package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import br.com.emmanuel.moneytransfer.domain.{Account, ClosedAccount, OpenedAccount}
import br.com.emmanuel.moneytransfer.event.AccountEvent._
import br.com.emmanuel.moneytransfer.event.Event
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor._
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Calendar

class AccountLedgerActorTest
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.defaultApplication()))
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command, Event, Account](
    system,
    AccountLedgerActor("1")
  )

  private val instant = Calendar.getInstance()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "Account" must {

    "be opened with zero balance" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      result.reply shouldBe StatusReply.Ack
      result.event shouldBe AccountOpened
      result.stateOfType[OpenedAccount].balance shouldBe 0
    }

    "handle deposit" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)

      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))

      result.reply shouldBe StatusReply.Ack
      result.stateOfType[OpenedAccount].balance shouldBe 100

      val depositedEvent = result.eventOfType[Deposited]
      depositedEvent.amount shouldBe 100
      depositedEvent.kind shouldBe "deposit"
      depositedEvent.instant shouldBe instant
    }

    "handle debit" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](Debit("debit", instant, 50, _))

      result.reply shouldBe StatusReply.Ack
      result.stateOfType[OpenedAccount].balance shouldBe 50

      val debitedEvent = result.eventOfType[Debited]
      debitedEvent.amount shouldBe 50
      debitedEvent.kind shouldBe "debit"
      debitedEvent.instant shouldBe instant
    }

    "reject debit overdraft" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](Debit("debit", instant, 200, _))

      result.reply.isError shouldBe true
      result.hasNoEvents shouldBe true
    }

    "handle close account" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](CloseAccount)

      result.reply shouldBe StatusReply.Ack
      result.event shouldBe a[AccountClosed.type]
      result.state shouldBe a[ClosedAccount]
    }

    "reject deposit when it's closed" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      eventSourcedTestKit.runCommand[StatusReply[Done]](CloseAccount)
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))

      result.reply.isError shouldBe true
      result.hasNoEvents shouldBe true
    }

    "reject deposit when it's not opened (empty)" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))

      result.reply.isError shouldBe true
      result.hasNoEvents shouldBe true
    }

    "handle get balance when it's closed" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      eventSourcedTestKit.runCommand[StatusReply[Done]](CloseAccount)

      val result = eventSourcedTestKit.runCommand[Reply](GetBalance)
      result.hasNoEvents shouldBe true

      val currentBalance = result.replyOfType[CurrentBalance]
      currentBalance.balance shouldBe  0
    }

    "handle get balance" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](OpenAccount)
      eventSourcedTestKit.runCommand[StatusReply[Done]](Credit("deposit", instant, 100, _))

      val result = eventSourcedTestKit.runCommand[Reply](GetBalance)
      result.hasNoEvents shouldBe true

      val currentBalance = result.replyOfType[CurrentBalance]
      currentBalance.balance shouldBe 100
    }

  }

}
