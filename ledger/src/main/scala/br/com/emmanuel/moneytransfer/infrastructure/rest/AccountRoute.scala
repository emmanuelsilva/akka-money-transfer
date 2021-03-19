package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.ledger.{Account, CreditTransaction, DebitTransaction}
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AccountRoute extends HasJsonSerializer {

  def route(bankActor: ActorRef[Command])(implicit scheduler: Scheduler,
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
                case Failure(exception)   => complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
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

        path(Segment / "credit") { accountId =>
          post {
            entity(as[CreditTransaction]) { transaction => {
              implicit val timeout: Timeout = 5.seconds
              val depositRequest = bankActor.ask(ref => Credit(transaction.kind, transaction.instant, Account(accountId), transaction.amount, ref))

              onComplete(depositRequest) {
                case Success(depositConfirmation) =>
                  depositConfirmation match {
                    case DepositConfirmed()       => complete(StatusCodes.OK)
                    case AccountNotFound(account) => complete(StatusCodes.NotFound, s"account ${account.id} not found")
                    case _                        => complete(StatusCodes.InternalServerError)
                  }
                case Failure(exception) =>
                  complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
              }
            }}
          }
        },

        path(Segment / "debit") { accountId =>
          post {
            entity(as[DebitTransaction]) { transaction => {
              implicit val timeout: Timeout = 5.seconds
              val withdrawRequest = bankActor.ask(ref => Debit(transaction.kind, transaction.instant, Account(accountId), transaction.amount, ref))

              onComplete(withdrawRequest) {
                case Success(withdrawConfirmation) =>
                  withdrawConfirmation match {
                    case WithdrawConfirmed()      => complete(StatusCodes.OK)
                    case AccountNotFound(account) => complete(StatusCodes.NotFound, s"account ${account.id} not found")
                    case InsufficientFunds(_,_)  => complete(StatusCodes.BadRequest, "insufficient funds")
                    case _                        => complete(StatusCodes.InternalServerError)
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
            val getAccountBalance: Future[Balance] = bankActor
              .ask(ref => GetAccountBalance(account, ref))
              .mapTo[Balance]

            onComplete(getAccountBalance) {
              case Success(accountBalance) => complete(Option(accountBalance))
              case Failure(exception)      => complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
            }
          }
        }

      )
    }
  }

}
