package br.com.emmanuel.moneytransfer.infrastructure.actors

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.{Accounts, CreateAccount, GetAccounts}
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class BankActorTest extends WordSpecLike with BeforeAndAfter {

  val testKit: ActorTestKit = ActorTestKit()
  var probe: TestProbe[BankActor.Response] = createBankResponseProbe

  override protected def before(fun: => Any)(implicit pos: Position): Unit = {
    probe = createBankResponseProbe
  }

  override protected def after(fun: => Any)(implicit pos: Position): Unit = {
    probe.stop()
  }

  "should create an account" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! GetAccounts(probe.ref)

    val getAccountsResponse = probe.expectMessageType[Accounts]
    assert(getAccountsResponse.accounts.contains(account))
  }

  private def createBankResponseProbe =
    testKit.createTestProbe[BankActor.Response]()
}
