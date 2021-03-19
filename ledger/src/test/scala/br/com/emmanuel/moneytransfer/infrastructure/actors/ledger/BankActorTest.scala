package br.com.emmanuel.moneytransfer.infrastructure.actors.ledger

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.ledger.Account
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import java.util.Calendar

class BankActorTest extends WordSpecLike with BeforeAndAfter with Matchers {

  val testKit: ActorTestKit = ActorTestKit()
  val instant: Calendar = Calendar.getInstance()
  var probe: TestProbe[Response] = _

  before {
    probe = createBankResponseProbe
  }

  after {
    probe.stop()
  }

  "should create an account" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! GetAccounts(probe.ref)

    val getAccountsResponse = probe.expectMessageType[Accounts]
    assertResult(1)(getAccountsResponse.accounts.size)
    assert(getAccountsResponse.accounts.contains(account))
  }

  "should return 100 after deposit 100" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Credit("deposit", instant, account, 100, probe.ref)
    bankActor ! GetAccountBalance(account, probe.ref)

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(Balance(account, 100))
  }

  "should confirm the withdraw if there's available balance" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Credit("deposit", instant, account, 100, probe.ref)
    bankActor ! Debit("withdraw", instant, account, 50, probe.ref)

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(WithdrawConfirmed())

    bankActor ! GetAccountBalance(account, probe.ref)
    probe.expectMessage(Balance(account, 50))
  }

  "should don't confirm the withdraw when there's no sufficient funds in the balance" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    val withDrawCommand = Debit("withdraw", instant, account, 500, probe.ref)

    bankActor ! CreateAccount(account)
    bankActor ! Credit("deposit", instant, account, 100, probe.ref)
    bankActor ! withDrawCommand

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(InsufficientFunds(account, withDrawCommand))

    bankActor ! GetAccountBalance(account, probe.ref)
    probe.expectMessage(Balance(account, 100))
  }

  private def createBankResponseProbe =
    testKit.createTestProbe[Response]()
}
