package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.Account
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.{Accounts, Command, GetAccounts}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class AccountRouteTest extends WordSpecLike with BeforeAndAfter with ScalatestRouteTest with Matchers with HasAccountJsonSerializer {

  import akka.actor.typed.scaladsl.adapter._
  implicit val typedSystem = system.toTyped
  implicit val timeout = Timeout(500.milliseconds)
  implicit val scheduler = system.scheduler

  val testKit: ActorTestKit = ActorTestKit()

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

  private def createMockAccounts(qtd: Int) = {
    (1 to qtd).map(_.toString).map(i => Account(s"account-${i}")).toSeq
  }

}
