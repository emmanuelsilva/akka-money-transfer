package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.AccountActor._
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class AccountActorTest extends WordSpecLike with BeforeAndAfter {

  val testKit: ActorTestKit = ActorTestKit()
  var probe: TestProbe[AccountActor.Response] = null

  before {
    probe = createAccountResponseProbe
  }

  after {
    probe.stop()
  }

  "should return all transactions for one account" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Deposit(100)
    accountActor ! Deposit(50)
    accountActor ! Withdraw(50, probe.ref)
    accountActor ! GetTransactions(probe.ref)

    val transactions = probe.expectMessageType[Transactions]
    assertResult(3)(transactions.transactions.size)
  }

  "should return insufficient funds when there is not balance to create P2P transfer" in {
    val sourceAccount = Account("123")
    val sourceAccountActor = createAccountActor(sourceAccount)

    val destinationAccount = Account("456")
    val destinationAccountActor = createAccountActor(destinationAccount)

    val p2pTransferCommand = P2PTransfer(100, AccountWithRef(destinationAccount, destinationAccountActor), probe.ref)
    sourceAccountActor ! p2pTransferCommand

    assertThatInsufficientFundsWasReceived(sourceAccount, p2pTransferCommand)

    sourceAccountActor ! GetBalance(probe.ref)
    assertThatBalanceForAccountIs(sourceAccount, 0)

    destinationAccountActor ! GetBalance(probe.ref)
    assertThatBalanceForAccountIs(destinationAccount, 0)
  }

  "should transfer 100 from account 123 to account 456" in {
    val sourceAccount = Account("123")
    val sourceAccountActor = createAccountActor(sourceAccount)

    val destinationAccount = Account("456")
    val destinationAccountActor = createAccountActor(destinationAccount)

    sourceAccountActor ! Deposit(100)
    sourceAccountActor ! P2PTransfer(100, AccountWithRef(destinationAccount, destinationAccountActor), probe.ref)

    sourceAccountActor ! GetBalance(probe.ref)
    assertThatBalanceForAccountIs(sourceAccount, 0)

    destinationAccountActor ! GetBalance(probe.ref)
    assertThatBalanceForAccountIs(destinationAccount, 100)
  }

  "should deposit 100, withdraw 100 and the balance is zero" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Deposit(100)
    accountActor ! Withdraw(100, probe.ref)

    accountActor ! GetBalance(probe.ref)
    assertThatBalanceForAccountIs(account, 0)
  }

  "should return insufficient funds when there is no available balance to withdraw operation" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    val withdrawCommand = Withdraw(100, probe.ref)
    accountActor ! withdrawCommand

    assertThatInsufficientFundsWasReceived(account, withdrawCommand)
  }

  "should deposit 100 and change balance amount to 100" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Deposit(100)
    accountActor ! GetBalance(probe.ref)

    assertThatBalanceForAccountIs(account, 100)
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

    accountActor ! AccountActor.GetBalance(probe.ref)
    assertThatBalanceForAccountIs(account, expectedFinalAmount)
  }

  "should retrieve balance when it is empty" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! AccountActor.GetBalance(probe.ref)
    assertThatBalanceForAccountIs(account, 0)
  }

  private def createAccountActor(account: Account) =
    testKit.spawn(AccountActor(account))


  private def createAccountResponseProbe =
    testKit.createTestProbe[AccountActor.Response]()


  private def assertThatBalanceForAccountIs(account: Account, expectedAmount: BigDecimal): Unit = {
    probe.expectMessage(Balance(account, expectedAmount))
  }

  private def assertThatInsufficientFundsWasReceived(account: Account, command: AccountActor.Command) = {
    probe.expectMessage(InsufficientFunds(account, command, "Insufficient funds"))
  }

}
