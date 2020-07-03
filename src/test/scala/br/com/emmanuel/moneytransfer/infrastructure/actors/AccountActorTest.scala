package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AccountActorTest extends WordSpecLike with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override protected def afterAll(): Unit = super.afterAll()

  "should receive deposit and change balance amount" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)

    accountActor ! AccountActor.Deposit(100)

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatExpectedBalanceIs(probe, accountId, 100)
  }

  "should receive all deposits message and change balance" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)
    val depositAmount = 100
    val depositQuantity = 1000

    for (_ <- 1 to depositQuantity) {
      accountActor ! AccountActor.Deposit(depositAmount)
    }

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatExpectedBalanceIs(probe, accountId, depositAmount * depositQuantity)
  }

  "should retrieve balance amount" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatExpectedBalanceIs(probe, accountId, 0)
  }

  private def createAccountActor(accountId: String) =
    testKit.spawn(AccountActor(accountId))


  private def createAccountResponseProbe =
    testKit.createTestProbe[AccountActor.Response]()


  private def assertThatExpectedBalanceIs(probe: TestProbe[AccountActor.Response],
                                          accountId: String,
                                          amount: BigDecimal): Unit = {
    probe.expectMessage(AccountActor.Balance(accountId, amount))
  }


}
