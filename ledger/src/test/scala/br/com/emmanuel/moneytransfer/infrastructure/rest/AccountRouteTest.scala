package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.Done
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.StatusReply
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor.CurrentBalance
import br.com.emmanuel.moneytransfer.infrastructure.factory.AccountTestEntityFactory
import br.com.emmanuel.moneytransfer.infrastructure.rest.request.{AccountRequest, CreditTransactionRequest, DebitTransactionRequest}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Calendar

class AccountRouteTest extends AnyWordSpecLike
                          with BeforeAndAfterEach
                          with ScalatestRouteTest
                          with ScalaFutures
                          with HasJsonSerializer {

  import akka.actor.typed.scaladsl.adapter._

  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
  var testKit: ActorTestKit = ActorTestKit()

  override def testConfig: Config = ConfigFactory.load("application-test.conf")

  override def beforeEach(): Unit = {
    testKit = ActorTestKit()
  }

  override def afterEach(): Unit = {
    testKit.shutdownTestKit()
  }

  "Account" must {
    "be created" in {
      val account = AccountRequest("123")
      val accountPostEntity = Marshal(account).to[MessageEntity].futureValue

      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))
      val testedRoute = Post("/accounts", accountPostEntity) ~> AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        status shouldEqual StatusCodes.Created
      }

      val replyProbe = testKit.createTestProbe[AccountLedgerActor.Reply]
      accountEntity ! AccountLedgerActor.GetBalance(replyProbe.ref)

      val currentBalance = replyProbe.expectMessageType[CurrentBalance]
      currentBalance.balance shouldBe 0
    }
  }

  "Credit" must {

    "return HTTP OK when credited into an existent account" in {
      val account = AccountRequest("123")
      val creditTransaction = CreditTransactionRequest("tid", "credit", Calendar.getInstance(), 100)
      val creditPostEntity = Marshal(creditTransaction).to[MessageEntity].futureValue
      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))

      openAccount(accountEntity)

      val testedRoute = Post(s"/accounts/${account.id}/credit", creditPostEntity) ~>
        AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        status shouldEqual StatusCodes.OK
      }

      assertThatCurrentBalanceIs(accountEntity, 100)
    }

    "return HTTP Bad response when credited into a non-existent account" in {
      val account = AccountRequest("123")
      val creditTransaction = CreditTransactionRequest("tid", "credit", Calendar.getInstance(), 100)
      val creditPostEntity = Marshal(creditTransaction).to[MessageEntity].futureValue
      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))

      val testedRoute = Post(s"/accounts/${account.id}/credit", creditPostEntity) ~>
        AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        responseAs[String] shouldBe "account=123 is not opened"
        status shouldEqual StatusCodes.BadRequest
      }
    }

  }

  "Debit" must {
    "return HTTP OK when there's enough balance " in {
      val account = AccountRequest("123")
      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))

      openAccount(accountEntity)
      deposit(accountEntity, 100)

      val debitTransaction = DebitTransactionRequest("deb123", "debit", Calendar.getInstance(), 50)
      val debitPostEntity = Marshal(debitTransaction).to[MessageEntity].futureValue

      val testedRoute = Post(s"/accounts/${account.id}/debit", debitPostEntity) ~>
        AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        status shouldEqual StatusCodes.OK
      }

      assertThatCurrentBalanceIs(accountEntity, 50)
    }

    "return HTTP BadRequest when overdraft" in {
      val account = AccountRequest("123")
      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))

      openAccount(accountEntity)
      deposit(accountEntity, 25)

      val debitTransaction = DebitTransactionRequest("deb123", "debit", Calendar.getInstance(), 100)
      val debitPostEntity = Marshal(debitTransaction).to[MessageEntity].futureValue

      val testedRoute = Post(s"/accounts/${account.id}/debit", debitPostEntity) ~>
        AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        responseAs[String] shouldBe "insufficient balance to debit 100"
        status shouldEqual StatusCodes.BadRequest
      }

      assertThatCurrentBalanceIs(accountEntity, 25)
    }
  }

  "Balance" must {
    "returned in HTTP OK with the current balance" in {
      val account = AccountRequest("123")
      val accountEntity = testKit.spawn(AccountLedgerActor(account.id))

      openAccount(accountEntity)
      deposit(accountEntity, 100)

      val testedRoute = Get(s"/accounts/${account.id}/balance") ~>
        AccountRoute.route(AccountTestEntityFactory(accountEntity))

      testedRoute ~> check {
        status shouldEqual StatusCodes.OK
        val currentBalance = responseAs[CurrentBalance]
        currentBalance.balance shouldBe 100
      }
    }
  }

  private def openAccount(accountEntity: ActorRef[AccountLedgerActor.Command]) = {
    val createAccountReplyProbe = testKit.createTestProbe[StatusReply[Done]]
    accountEntity ! AccountLedgerActor.OpenAccount(createAccountReplyProbe.ref)
  }

  private def deposit(accountEntity: ActorRef[AccountLedgerActor.Command], amount: BigDecimal): Unit = {
    val creditReplyProbe = testKit.createTestProbe[StatusReply[Done]]
    accountEntity ! AccountLedgerActor.Credit("credit", Calendar.getInstance(), amount, creditReplyProbe.ref)
  }

  private def assertThatCurrentBalanceIs(accountEntity: ActorRef[AccountLedgerActor.Command], amount: BigDecimal) = {
    val replyProbe = testKit.createTestProbe[AccountLedgerActor.Reply]
    accountEntity ! AccountLedgerActor.GetBalance(replyProbe.ref)

    val currentBalance = replyProbe.expectMessageType[CurrentBalance]
    currentBalance.balance shouldBe amount
  }
}
