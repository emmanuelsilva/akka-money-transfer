package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AccountActorTest extends WordSpecLike with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override protected def afterAll(): Unit = super.afterAll()

  "should receive deposit and change balance amount" in {
    val accountId = "123"
    val accountActor = testKit.spawn(AccountActor(accountId))

    accountActor ! AccountActor.Deposit(100)

    val probe = testKit.createTestProbe[AccountActor.Response]()
    accountActor ! AccountActor.GetBalance(probe.ref)

    probe.expectMessage(AccountActor.Balance(accountId, 100))
  }

  "should retrieve balance amount" in {
    val accountId = "123"
    val accountActor = testKit.spawn(AccountActor(accountId))

    val probe = testKit.createTestProbe[AccountActor.Response]()
    accountActor ! AccountActor.GetBalance(probe.ref)

    probe.expectMessage(AccountActor.Balance(accountId, 0))
  }

}
