package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AccountActorTest extends WordSpecLike with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override protected def afterAll(): Unit = super.afterAll()

  "should deposit 100, withdraw 100 and the balance is zero" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)
    val probe = createAccountResponseProbe

    accountActor ! AccountActor.Deposit(100)
    accountActor ! AccountActor.Withdraw(100, probe.ref)

    accountActor ! AccountActor.GetBalance(probe.ref)
    assertThatBalanceIs(probe, accountId, 0)
  }

  "should return insufficient funds when there is no available balance to withdraw operation" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)
    val probe = createAccountResponseProbe

    val withdrawCommand = AccountActor.Withdraw(100, probe.ref)
    accountActor ! withdrawCommand

    assertThatInsufficientFundsWasReceived(accountId, probe, withdrawCommand)
  }

  "should deposit 100 and change balance amount to 100" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)

    accountActor ! AccountActor.Deposit(100)

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatBalanceIs(probe, accountId, 100)
  }

  "should receive 1000 deposits and change balance amount to 100000" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)
    val depositAmount = 100
    val depositQuantity = 1000
    val expectedFinalAmount = depositAmount * depositQuantity

    for (_ <- 1 to depositQuantity) {
      accountActor ! AccountActor.Deposit(depositAmount)
    }

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatBalanceIs(probe, accountId, expectedFinalAmount)
  }

  "should retrieve balance when it is empty" in {
    val accountId = "123"
    val accountActor = createAccountActor(accountId)

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatBalanceIs(probe, accountId, 0)
  }

  private def createAccountActor(accountId: String) =
    testKit.spawn(AccountActor(accountId))


  private def createAccountResponseProbe =
    testKit.createTestProbe[AccountActor.Response]()


  private def assertThatBalanceIs(probe: TestProbe[AccountActor.Response],
                                          accountId: String,
                                          amount: BigDecimal): Unit = {
    probe.expectMessage(AccountActor.Balance(accountId, amount))
  }

  private def assertThatInsufficientFundsWasReceived(accountId: String,
                                                     probe: TestProbe[AccountActor.Response],
                                                     command: AccountActor.Command) = {
    probe.expectMessage(AccountActor.InsufficientFunds(accountId, command))
  }

}
