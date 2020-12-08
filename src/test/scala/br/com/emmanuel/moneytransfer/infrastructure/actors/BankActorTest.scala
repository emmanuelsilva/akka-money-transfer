package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.{Accounts, CreateAccount, GetAccounts}
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class BankActorTest extends WordSpecLike with BeforeAndAfter {

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

  private def createBankResponseProbe =
    testKit.createTestProbe[BankActor.Response]()
}
