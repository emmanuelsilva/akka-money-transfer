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

  "should return 100 after deposit 100" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Deposit(100, account, probe.ref)
    bankActor ! GetAccountBalance(account, probe.ref)

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(AccountBalance(account, 100))
  }

  "should confirm the withdraw if there's available balance" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Deposit(100, account, probe.ref)
    bankActor ! Withdraw(50, account, probe.ref)

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(WithdrawConfirmed())

    bankActor ! GetAccountBalance(account, probe.ref)
    probe.expectMessage(AccountBalance(account, 50))
  }

  "should dont confirm the withdraw when there's no sufficient funds in the balance" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Deposit(100, account, probe.ref)
    bankActor ! Withdraw(500, account, probe.ref)

    probe.expectMessage(DepositConfirmed())
    probe.expectMessage(InsufficientFunds("Can't complete the withdraw due to insufficient funds"))

    bankActor ! GetAccountBalance(account, probe.ref)
    probe.expectMessage(AccountBalance(account, 100))
  }

  "should use the P2P command to transfer the 100 from Account 123 to Account 456" in {
    val bankActor = testKit.spawn(BankActor())

    val fromAccount = Account("123")
    val toAccount = Account("456")

    bankActor ! CreateAccount(fromAccount)
    bankActor ! CreateAccount(toAccount)

    bankActor ! Deposit(100, fromAccount, probe.ref)
    probe.expectMessage(DepositConfirmed())

    bankActor ! P2P(100, fromAccount, toAccount, probe.ref)
    probe.expectMessage(P2PConfirmed())

    bankActor ! GetAccountBalance(fromAccount, probe.ref)
    probe.expectMessage(AccountBalance(fromAccount, 0))

    bankActor ! GetAccountBalance(toAccount, probe.ref)
    probe.expectMessage(AccountBalance(toAccount, 100))
  }

  "should expect AccountNotFound when a P2P destination account doesn't exist" in {
    val bankActor = testKit.spawn(BankActor())

    val fromAccount = Account("123")
    bankActor ! CreateAccount(fromAccount)

    val dontExistentAccount = Account("456")

    bankActor ! P2P(100, fromAccount, dontExistentAccount, probe.ref)
    probe.expectMessage(AccountNotFound(dontExistentAccount))
  }

  "should expect AccountNotFound when a P2P origin account doesn't exist" in {
    val bankActor = testKit.spawn(BankActor())

    val fromAccount = Account("123")

    val toAccount = Account("456")
    bankActor ! CreateAccount(toAccount)

    bankActor ! P2P(100, fromAccount, toAccount, probe.ref)
    probe.expectMessage(AccountNotFound(fromAccount))
  }

  "should fail the p2p transaction when the origin account doesn't have enough funds in the balance" in {
    val bankActor = testKit.spawn(BankActor())

    val fromAccount = Account("123")
    val toAccount = Account("456")

    bankActor ! CreateAccount(fromAccount)
    bankActor ! CreateAccount(toAccount)

    bankActor ! P2P(100, fromAccount, toAccount, probe.ref)
    probe.expectMessage(P2PFailed())
  }

  private def createBankResponseProbe =
    testKit.createTestProbe[BankActor.Response]()
}
