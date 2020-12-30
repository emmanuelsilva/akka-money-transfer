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
import br.com.emmanuel.moneytransfer.domain.{Account, DepositTransaction, WithdrawTransaction}
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class AccountRouteTest extends WordSpecLike with BeforeAndAfter with ScalatestRouteTest with Matchers with HasJsonSerializer {

  import akka.actor.typed.scaladsl.adapter._
  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
  implicit val timeout: Timeout = Timeout(500.milliseconds)
  implicit val scheduler: Scheduler = system.scheduler

  val testKit: ActorTestKit = ActorTestKit()

  "post /accounts/123/withdraw should withdraw from the 123 balance's account" in {
    val bankActor = testKit.spawn(BankActor())
    val probe = testKit.createTestProbe[Response]

    val account = Account("123")

    bankActor ! CreateAccount(account)
    bankActor ! Deposit(100, account, probe.ref)

    val withdrawTransaction = WithdrawTransaction(account, 50)
    val withdrawPostEntity = Marshal(withdrawTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/withdraw", withdrawPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      assertThatBalanceIs(bankActor, account, 50)
    }
  }

  "deposit into non existent account should return 404" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")

    val depositTransaction = DepositTransaction(account, 100)
    val depositPostEntity = Marshal(depositTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/deposit", depositPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "post /accounts/123/deposit should deposit into the 123 balance's account" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")
    bankActor ! CreateAccount(account)
    val depositTransaction = DepositTransaction(account, 100)
    val depositPostEntity = Marshal(depositTransaction).to[MessageEntity].futureValue

    val testedRoute = Post("/accounts/123/deposit", depositPostEntity) ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.Created
      assertThatBalanceIs(bankActor, account, 100)
    }
  }

  "get /accounts/123/balance should return zero balance when there is no transactions" in {
    val bankActor = testKit.spawn(BankActor())
    val account = Account("123")
    bankActor ! CreateAccount(account)

    val testedRoute = Get("/accounts/123/balance") ~> AccountRoute.route(bankActor)

    testedRoute ~> check {
      status shouldEqual StatusCodes.OK
      val accountBalance = responseAs[AccountBalance]
      accountBalance.account shouldBe account
      accountBalance.balance shouldBe 0
    }
  }

  "post /accounts with valid body should create account" in {
    val bankActor = testKit.spawn(BankActor())
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

  "get /accounts should return 10 registered accounts" in {
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

  "get /accounts should return 404 when there is not no one account" in {
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
    val accountBalanceMessage = probe.expectMessageType[AccountBalance]

    accountBalanceMessage.account shouldBe account
    accountBalanceMessage.balance shouldBe expectedBalance
  }

  private def createMockAccounts(qtd: Int) = {
    (1 to qtd).map(_.toString).map(i => Account(s"account-$i"))
  }

}
