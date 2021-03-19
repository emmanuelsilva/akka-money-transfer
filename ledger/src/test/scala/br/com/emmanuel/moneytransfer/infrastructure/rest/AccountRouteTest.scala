package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol.{Balance, Accounts, Command, CreateAccount, Credit, GetAccountBalance, GetAccounts, Response}
import br.com.emmanuel.moneytransfer.domain.ledger.{Account, CreditTransaction, DebitTransaction}
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.BankLedgerActor
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import java.util.UUID
import scala.concurrent.duration.DurationInt

class AccountRouteTest extends WordSpecLike with BeforeAndAfter with ScalatestRouteTest with Matchers with HasJsonSerializer {

  import akka.actor.typed.scaladsl.adapter._
  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
  implicit val timeout: Timeout = Timeout(500.milliseconds)
  implicit val scheduler: Scheduler = system.scheduler

  def generateId: String = UUID.randomUUID().toString

  def createDebitTransaction(account: Account, amount: BigDecimal): DebitTransaction = {
    val now = java.util.Calendar.getInstance()
    DebitTransaction(generateId, "debit", now, account, amount)
  }

  def createCreditTransaction(account: Account, amount: BigDecimal): CreditTransaction = {
    val now = java.util.Calendar.getInstance()
    CreditTransaction(generateId, "credit", now, account, amount)
  }

  def createCreditCommand(account: Account, amount: BigDecimal, replyTo: ActorRef[Response]): Credit = {
    val now = java.util.Calendar.getInstance()
    Credit("credit", now, account, amount, replyTo)
  }

  val testKit: ActorTestKit = ActorTestKit()

  "post /accounts/{accountId}/debit for a non-existent account should return not found" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")
    val debitTransaction = createDebitTransaction(account, 500)
    val debitPostEntity = Marshal(debitTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/debit", debitPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "post /accounts/{accountId}/debit should return bad request when account has no enough balance" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val probe = testKit.createTestProbe[Response]

    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! createCreditCommand(account, 100, probe.ref)

    val debitTransaction = createDebitTransaction(account, 500)
    val debitPostEntity = Marshal(debitTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/debit", debitPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
      assertThatBalanceIs(bankActor, account, 100)
    }
  }

  "post /accounts/{accountId}/debit should debit from the balance account when there is available balance" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val probe = testKit.createTestProbe[Response]

    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! createCreditCommand(account, 100, probe.ref)

    val debitTransaction = createDebitTransaction(account, 50)
    val debitPostEntity = Marshal(debitTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/debit", debitPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      assertThatBalanceIs(bankActor, account, 50)
    }
  }

  "post /accounts/{accountId}/credit should return 404 when the account doesn't exist" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    val creditTransaction = createCreditTransaction(account, 100)
    val creditPostEntity = Marshal(creditTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/credit", creditPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "post /accounts/{accountId}/credit should credit into the existent account balance" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")

    bankActor ! CreateAccount(account)
    val creditTransaction = createCreditTransaction(account, 100)
    val creditPostEntity = Marshal(creditTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/credit", creditPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      assertThatBalanceIs(bankActor, account, 100)
    }
  }

  "get /accounts/{accountId}/balance should return zero balance when there is no transactions for the account" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val account = Account("123")
    bankActor ! CreateAccount(account)

    val testedRoute = Get("/accounts/123/balance") ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      val accountBalance = responseAs[Balance]
      accountBalance.account shouldBe account
      accountBalance.amount shouldBe 0
    }
  }

  "post /accounts with valid body should create an account" in {
    val bankActor = testKit.spawn(BankLedgerActor())
    val probe = testKit.createTestProbe[Response]
    val account = Account("123")
    val accountPostEntity = Marshal(account).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts", accountPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.Created
    }

    bankActor ! GetAccounts(probe.ref)
    val getAllAccountsResponse = probe.expectMessageType[Accounts]
    assertResult(1)(getAllAccountsResponse.accounts.size)
    assert(getAllAccountsResponse.accounts.contains(account))
  }

  "get /accounts should all registered accounts" in {
    val probe = testKit.createTestProbe[Command]
    val testedRoute = Get("/accounts") ~> AccountRoute.route(probe.ref)

    val getAccountsMessage = probe.expectMessageType[GetAccounts]
    getAccountsMessage.reply ! Accounts(createMockAccounts(10))

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      val body = responseAs[Accounts]
      assertResult(10)(body.accounts.size)
    }
  }

  "get /accounts should return 404 when there is no one account" in {
    val probe = testKit.createTestProbe[Command]
    val testedRoute = Get("/accounts") ~> Route.seal(AccountRoute.route(probe.ref))

    val getAccountsMessage = probe.expectMessageType[GetAccounts]
    getAccountsMessage.reply ! Accounts(Seq[Account]())

    testedRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  private def assertThatBalanceIs(bankActor: ActorRef[Command], account: Account, expectedBalance: BigDecimal): Unit = {
    val probe = testKit.createTestProbe[Response]
    bankActor ! GetAccountBalance(account, probe.ref)
    val accountBalanceMessage = probe.expectMessageType[Balance]

    accountBalanceMessage.account shouldBe account
    accountBalanceMessage.amount shouldBe expectedBalance
  }

  private def createMockAccounts(qtd: Int) = (1 to qtd).map(_.toString).map(i => Account(s"account-$i"))

}
