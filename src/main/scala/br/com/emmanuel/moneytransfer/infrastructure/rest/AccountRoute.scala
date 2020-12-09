package br.com.emmanuel.moneytransfer.infrastructure.rest

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, onComplete, path, rejectEmptyResponse}
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.BankActor.{Accounts, GetAccounts}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AccountRoute extends HasAccountJsonSerializer {

  def route(bankActor: ActorRef[BankActor.Command])(implicit scheduler: Scheduler,
                                                    executionContext: ExecutionContext) = rejectEmptyResponse {
    path("accounts") {
      get {
        implicit val timeout: Timeout = 5.seconds
        val futureAccounts: Future[Accounts] = (bankActor ? GetAccounts).mapTo[Accounts]

        onComplete(futureAccounts) {
          case Success(getAccounts) => complete(Option(getAccounts).filter(_.accounts.nonEmpty))
          case Failure(exception) => complete(StatusCodes.InternalServerError, s"error: ${exception.getMessage}")
        }
      }
    }
  }

}
