package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.AccountActor._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AccountActorTest extends WordSpecLike with BeforeAndAfterAll {

  val testKit: ActorTestKit = ActorTestKit()

  override protected def afterAll(): Unit = super.afterAll()

  "should return insufficient funds when there is not balance to create P2P transfer" in {
    val probe = createAccountResponseProbe

    val sourceAccount = Account("123")
    val sourceAccountActor = createAccountActor(sourceAccount)

    val destinationAccount = Account("456")
    val destinationAccountActor = createAccountActor(destinationAccount)

    val p2pTransferCommand = P2PTransfer(100, AccountRef(destinationAccount, destinationAccountActor), probe.ref)
    sourceAccountActor ! p2pTransferCommand

    assertThatInsufficientFundsWasReceived(sourceAccount, probe, p2pTransferCommand)

    sourceAccountActor ! GetBalance(probe.ref)
    assertThatBalanceIs(probe, sourceAccount, 0)

    destinationAccountActor ! GetBalance(probe.ref)
    assertThatBalanceIs(probe, destinationAccount, 0)
  }

  "should transfer 100 from account 123 to account 456" in {
    val probe = createAccountResponseProbe

    val sourceAccount = Account("123")
    val sourceAccountActor = createAccountActor(sourceAccount)

    val destinationAccount = Account("456")
    val destinationAccountActor = createAccountActor(destinationAccount)

    sourceAccountActor ! Deposit(100)
    sourceAccountActor ! P2PTransfer(100, AccountRef(destinationAccount, destinationAccountActor), probe.ref)

    sourceAccountActor ! GetBalance(probe.ref)
    assertThatBalanceIs(probe, sourceAccount, 0)

    destinationAccountActor ! GetBalance(probe.ref)
    assertThatBalanceIs(probe, destinationAccount, 100)
  }

  "should deposit 100, withdraw 100 and the balance is zero" in {
    val probe = createAccountResponseProbe
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Deposit(100)
    accountActor ! Withdraw(100, probe.ref)

    accountActor ! GetBalance(probe.ref)
    assertThatBalanceIs(probe, account, 0)
  }

  "should return insufficient funds when there is no available balance to withdraw operation" in {
    val probe = createAccountResponseProbe
    val account = Account("123")
    val accountActor = createAccountActor(account)

    val withdrawCommand = Withdraw(100, probe.ref)
    accountActor ! withdrawCommand

    assertThatInsufficientFundsWasReceived(account, probe, withdrawCommand)
  }

  "should deposit 100 and change balance amount to 100" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Deposit(100)

    val probe = createAccountResponseProbe
    accountActor ! GetBalance(probe.ref)

    assertThatBalanceIs(probe, account, 100)
  }

  "should receive 1000 deposits and change balance amount to 100000" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)
    val depositAmount = 100
    val depositQuantity = 1000
    val expectedFinalAmount = depositAmount * depositQuantity

    for (_ <- 1 to depositQuantity) {
      accountActor ! AccountActor.Deposit(depositAmount)
    }

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatBalanceIs(probe, account, expectedFinalAmount)
  }

  "should retrieve balance when it is empty" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    val probe = createAccountResponseProbe
    accountActor ! AccountActor.GetBalance(probe.ref)

    assertThatBalanceIs(probe, account, 0)
  }

  private def createAccountActor(account: Account) =
    testKit.spawn(AccountActor(account))


  private def createAccountResponseProbe =
    testKit.createTestProbe[AccountActor.Response]()


  private def assertThatBalanceIs(probe: TestProbe[AccountActor.Response],
                                          account: Account,
                                          amount: BigDecimal): Unit = {
    probe.expectMessage(AccountActor.Balance(account, amount))
  }

  private def assertThatInsufficientFundsWasReceived(account: Account,
                                                     probe: TestProbe[AccountActor.Response],
                                                     command: AccountActor.Command) = {
    probe.expectMessage(AccountActor.InsufficientFunds(account, command))
  }

}
