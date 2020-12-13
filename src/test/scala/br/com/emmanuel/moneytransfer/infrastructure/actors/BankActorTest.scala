package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

class BankActorTest extends WordSpecLike with BeforeAndAfter with Matchers {

  val testKit: ActorTestKit = ActorTestKit()
  var probe: TestProbe[BankActor.Response] = null

  before {
    probe = createBankResponseProbe
  }

  after {
    probe.stop()
  }

  "should create an account" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! GetAccounts(probe.ref)

    val getAccountsResponse = probe.expectMessageType[Accounts]

    assertResult(1)(getAccountsResponse.accounts.size)
    assert(getAccountsResponse.accounts.contains(account))
  }

  "should return 100 in the account balance after deposit 100" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Deposit(100, account)
    bankActor ! GetAccountBalance(account, probe.ref)

    val accountBalance = probe.expectMessageType[AccountBalance]
    accountBalance.account shouldBe account
    accountBalance.balance shouldBe 100
  }

  private def createBankResponseProbe =
    testKit.createTestProbe[BankActor.Response]()
}
