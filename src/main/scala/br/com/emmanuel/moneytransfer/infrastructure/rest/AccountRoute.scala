package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.{Account, DepositTransaction}
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AccountRoute extends HasJsonSerializer {

  def route(bankActor: ActorRef[BankActor.Command])(implicit scheduler: Scheduler,
                                                    executionContext: ExecutionContext) = rejectEmptyResponse {
    pathPrefix("accounts") {
      concat(
        pathEnd {
          concat(
            get {
              implicit val timeout: Timeout = 5.seconds
              val futureAccounts: Future[Accounts] = (bankActor ? GetAccounts).mapTo[Accounts]

              onComplete(futureAccounts) {
                case Success(getAccounts) => complete(Option(getAccounts).filter(_.accounts.nonEmpty))
                case Failure(exception) => complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
              }
            },

            post {
              entity(as[Account]) { account => {
                bankActor ! CreateAccount(account)
                complete(StatusCodes.Created)
              }}
            },
          )
        },

        path(Segment / "deposit") { accountId =>
          post {
            entity(as[DepositTransaction]) { transaction => {
              implicit val timeout: Timeout = 5.seconds
              val depositConfirmation = bankActor.ask(ref => Deposit(transaction.amount, Account(accountId), ref))

              onComplete(depositConfirmation) {
                case Success(response) =>
                  response match {
                    case DepositConfirmed() =>
                      complete(StatusCodes.Created)
                    case AccountNotFound(account) =>
                      complete(StatusCodes.NotFound, s"account ${account.id} not found")
                    case _ =>
                      complete(StatusCodes.InternalServerError)
                  }
                case Failure(exception) =>
                  complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
              }
            }}
          }
        },

        path(Segment / "balance") { accountId =>
          get {
            implicit val timeout: Timeout = 5.seconds

            val account = Account(accountId)
            val getAccountBalance: Future[AccountBalance] = bankActor
              .ask(ref => GetAccountBalance(account, ref))
              .mapTo[AccountBalance]

            onComplete(getAccountBalance) {
              case Success(accountBalance) => complete(Option(accountBalance))
              case Failure(exception) => complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
            }
          }
        }

      )
    }
  }

}
