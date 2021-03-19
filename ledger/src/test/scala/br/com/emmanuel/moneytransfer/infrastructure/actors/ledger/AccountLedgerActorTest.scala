package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.ledger.Account
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol._
import org.scalatest.{BeforeAndAfter, WordSpecLike}

import java.util.Calendar

class AccountLedgerActorTest extends WordSpecLike with BeforeAndAfter {

  val testKit: ActorTestKit = ActorTestKit()
  val instant: Calendar = Calendar.getInstance()
  var probe: TestProbe[Response] = _

  before {
    probe = createAccountResponseProbe
  }

  after {
    probe.stop()
  }

  "should return all transactions for one account" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Credit("deposit", instant, account, 100, probe.ref)
    probe.expectMessage(DepositConfirmed())

    accountActor ! Credit("deposit", instant, account, 50, probe.ref)
    probe.expectMessage(DepositConfirmed())

    accountActor ! Debit("withdraw", instant, account, 50, probe.ref)
    probe.expectMessage(WithdrawConfirmed())

    accountActor ! GetAllTransactions(account, probe.ref)
    val transactions = probe.expectMessageType[Transactions]
    assertResult(3)(transactions.transactions.size)
  }

  "deposit 100, withdraw 100 and the balance should be zero" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Credit("deposit", instant, account, 100, probe.ref)
    probe.expectMessage(DepositConfirmed())

    accountActor ! Debit("withdraw", instant, account, 100, probe.ref)
    probe.expectMessage(WithdrawConfirmed())

    accountActor ! GetAccountBalance(account, probe.ref)
    assertThatBalanceForAccountIs(account, 0)
  }

  "should return insufficient funds when there is no available balance to withdraw operation" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    val withdrawCommand = Debit("withdraw", instant, account, 100, probe.ref)
    accountActor ! withdrawCommand

    assertThatInsufficientFundsWasReceived(account, withdrawCommand)
  }

  "deposit 100 and balance amount should be 100" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! Credit("deposit", instant, account, 100, probe.ref)
    probe.expectMessage(DepositConfirmed())

    accountActor ! GetAccountBalance(account, probe.ref)
    assertThatBalanceForAccountIs(account, 100)
  }

  "receive 1000 deposits and balance amount should be 100000" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)
    val depositAmount = 100
    val depositQuantity = 1000
    val expectedFinalAmount = depositAmount * depositQuantity

    for (_ <- 1 to depositQuantity) {
      accountActor ! Credit("deposit", instant, account, depositAmount, probe.ref)
      probe.expectMessage(DepositConfirmed())
    }

    accountActor ! GetAccountBalance(account, probe.ref)
    assertThatBalanceForAccountIs(account, expectedFinalAmount)
  }

  "should retrieve balance when it is empty" in {
    val account = Account("123")
    val accountActor = createAccountActor(account)

    accountActor ! GetAccountBalance(account, probe.ref)
    assertThatBalanceForAccountIs(account, 0)
  }

  private def createAccountActor(account: Account) =
    testKit.spawn(AccountLedgerActor(account))


  private def createAccountResponseProbe =
    testKit.createTestProbe[Response]()


  private def assertThatBalanceForAccountIs(account: Account, expectedAmount: BigDecimal): Unit = {
    probe.expectMessage(Balance(account, expectedAmount))
  }

  private def assertThatInsufficientFundsWasReceived(account: Account, command: Command) = {
    probe.expectMessage(InsufficientFunds(account, command))
  }

}
