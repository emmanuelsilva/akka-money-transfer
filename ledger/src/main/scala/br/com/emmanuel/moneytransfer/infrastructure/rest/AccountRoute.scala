package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.Scheduler
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.infrastructure.actors.factory.AccountEntityFactory
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerEntityActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerEntityActor.CurrentBalance
import br.com.emmanuel.moneytransfer.infrastructure.rest.request.{AccountRequest, CreditTransactionRequest, DebitTransactionRequest}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AccountRoute extends HasJsonSerializer {

  implicit val timeout: Timeout = 30.seconds

  def route(factory: AccountEntityFactory)(implicit scheduler: Scheduler,
                                           executionContext: ExecutionContext): Route = rejectEmptyResponse {
    pathPrefix("accounts") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[AccountRequest]) { account => {
                val accountEntity = factory.getAccountEntity(account.id)
                val request = accountEntity.askWithStatus(rep => AccountLedgerEntityActor.OpenAccount(rep))

                onComplete(request) {
                  case Success(_)                             => complete(StatusCodes.Created)
                  case Failure(StatusReply.ErrorMessage(msg)) => complete(StatusCodes.BadRequest, msg)
                  case Failure(error)                         =>
                    error.printStackTrace()
                    complete(StatusCodes.InternalServerError, error.getMessage)
                }
              }}
            },
          )
        },

        path(Segment / "credit") { accountId =>
          post {
            entity(as[CreditTransactionRequest]) { transaction => {
              val accountEntity = factory.getAccountEntity(accountId)
              val request = accountEntity.askWithStatus(ref => AccountLedgerEntityActor.Credit(transaction.kind, transaction.instant, transaction.amount, ref))

              onComplete(request) {
                case Success(_)                             => complete(StatusCodes.OK)
                case Failure(StatusReply.ErrorMessage(msg)) => complete(StatusCodes.BadRequest, msg)
                case Failure(error)                         => complete(StatusCodes.InternalServerError, error.getMessage)
              }

            }}
          }
        },

        path(Segment / "debit") { accountId =>
          post {
            entity(as[DebitTransactionRequest]) { transaction => {
              val accountEntity = factory.getAccountEntity(accountId)
              val request = accountEntity.askWithStatus(ref => AccountLedgerEntityActor.Debit(transaction.kind, transaction.instant, transaction.amount, ref))

              onComplete(request) {
                case Success(_)                             => complete(StatusCodes.OK)
                case Failure(StatusReply.ErrorMessage(msg)) => complete(StatusCodes.BadRequest, msg)
                case Failure(error)                         => complete(StatusCodes.InternalServerError, error.getMessage)
              }
            }}
          }
        },

        path(Segment / "balance") { accountId =>
          get {
            val accountEntity = factory.getAccountEntity(accountId)
            val request = accountEntity.ask(ref => AccountLedgerEntityActor.GetBalance(ref))

            onComplete(request) {
              case Success(currentBalance) => {
                currentBalance match {
                  case response: CurrentBalance => complete(Option(response))
                  case _                        => complete(StatusCodes.BadRequest, "response not mapped")
                }
              }
              case Failure(error) => complete(StatusCodes.BadRequest, error.getMessage)
            }
          }
        }

      )
    }
  }

}
